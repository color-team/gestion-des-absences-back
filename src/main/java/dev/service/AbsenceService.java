/**
 * 
 */
package dev.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.transaction.Transactional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import dev.controller.vm.AbsenceVM;
import dev.domain.Absence;
import dev.domain.Collegue;
import dev.domain.enumerations.Departement;
import dev.domain.enumerations.Role;
import dev.domain.enumerations.Status;
import dev.repository.AbsenceRepo;
import dev.repository.CollegueRepo;

/**
 * @author robin
 *
 */
@Service
@Transactional
public class AbsenceService extends LogService {

	private AbsenceRepo absenceRepo;

	public AbsenceService(AbsenceRepo absenceRepo, CollegueRepo collegueRepo) {
		super(collegueRepo);
		this.absenceRepo = absenceRepo;
	}

	public List<AbsenceVM> findAbsences() {

		Optional<Collegue> col = this.getColConnecte();

		if (col.isPresent()) {

			List<Absence> tmp = this.absenceRepo.findAbsences(col.get());
			List<AbsenceVM> res = new ArrayList<>();

			for (Absence a : tmp) {
				res.add(new AbsenceVM(a.getUuid(), a.getDateDebut(), a.getDateFin(), a.getType(), a.getStatus(),
						a.getMotif()));
			}
			return res;
		}
		throw new RuntimeException("Error col non connecté -  find absence");
	}

	public void deleteAbs(UUID uuid) {
		Optional<Collegue> col = this.getColConnecte();
		if (col.isPresent()) {
			this.absenceRepo.deleteAbs(uuid, col.get());
		} else {
			throw new RuntimeException("Error col non connecté - delete absence");
		}
	}

	public ResponseEntity<?> patchAbs(AbsenceVM updateAbs) {
		Optional<Collegue> col = getColConnecte();
		if (col.isPresent()) {
			
			List<Absence> listOldAbsence = this.absenceRepo.findAbsences(col.get());
			boolean valide = true;

			for (Absence absenceOld : listOldAbsence) {

				if (!(updateAbs.getDateFin().isBefore(absenceOld.getDateDebut())
						|| updateAbs.getDateDebut().isAfter(absenceOld.getDateFin())) && !(updateAbs.getUuid().equals(absenceOld.getUuid()))) {

					valide = false;
				}
			}
			
			 if (valide) {
				 this.absenceRepo.patchAbs(updateAbs.getDateDebut(), updateAbs.getDateFin(), updateAbs.getType(), updateAbs.getMotif(), updateAbs.getUuid(), col.get());
				 return ResponseEntity.status(HttpStatus.OK).body("");	 
			 }
			 return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Dates se chevauchent");
			
		}
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Collegue non présent");
		
		
	}
	
	public ResponseEntity<?> saveAbs(AbsenceVM absenceNew) {
		Optional<Collegue> col = getColConnecte();

		if (col.isPresent()) {
			
			List<Absence> listOldAbsence = this.absenceRepo.findAbsences(col.get());
			boolean valide = true;

			for (Absence absenceOld : listOldAbsence) {

				if (!(absenceNew.getDateFin().isBefore(absenceOld.getDateDebut())
						|| absenceNew.getDateDebut().isAfter(absenceOld.getDateFin()))) {

					valide = false;
				}
			}
			
			if (valide) {
				Absence tmp = absenceRepo.save(new Absence(absenceNew.getDateDebut(), absenceNew.getDateFin(),
						absenceNew.getType(), Status.STATUS_INITIAL, absenceNew.getMotif(), col.get()));
				AbsenceVM abspost = new AbsenceVM(tmp.getDateDebut(), tmp.getDateFin(), tmp.getType(),
						tmp.getMotif());

				return ResponseEntity.status(HttpStatus.OK).body(abspost);
			}

		}		
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Dates se chevauchent");
	}

	public List<AbsenceVM> findAbsenceMoisAnnee(int mois, int annee){
		
		List<Absence> tmp = this.absenceRepo.findAbsenceMoisAnnee(mois, annee);
		List<AbsenceVM> resultatsAbs = new ArrayList<>();
     
	    tmp.forEach(abs -> {
	         resultatsAbs.add(new AbsenceVM(abs));
	    });
	    return resultatsAbs;
	}

	public List<AbsenceVM> getAbsencesValideeMoisAnneeDepartement(int mois, int annee, Departement departement) {
		
		Optional<Collegue> col = this.getColConnecte();
		
		if (col.isPresent() && col.get().getRoles().get(0).getRole() == Role.ROLE_MANAGER) {

			List<Absence> absMoisAnneeDepartement = absenceRepo.findAbsencesValideeMoisAnneeDepartement(mois, annee, departement);
			List<AbsenceVM> resultat = new ArrayList<>();

			absMoisAnneeDepartement.forEach(a -> {
				resultat.add(new AbsenceVM(a));
			});
			return resultat;

		}
		throw new RuntimeException("Error col non connecté ou vous n'êtes pas un manager et donc vous n'êtes pas autorisé");
	}
}
