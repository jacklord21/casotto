package it.unicam.cs.ids.Casotto.Classi;

import it.unicam.cs.ids.Casotto.Repository.OrdinazioneRepository;
import it.unicam.cs.ids.Casotto.Repository.RichiestaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class GestoreOrdinazione {

    private final GestoreProdotti gestoreProdotti;

    @Autowired
    OrdinazioneRepository ordinazioneRepository;

    @Autowired
    RichiestaRepository richiestaRepository;

    GestoreOrdinazione(){
        this.gestoreProdotti = new GestoreProdotti();
    }

    public List<Richiesta> getRichiesteOf(Ordinazione ordinazione){
        this.checkIsNull(ordinazione);
        if(!ordinazioneRepository.existsById(ordinazione.getId())){
            throw new IllegalArgumentException("L'ordinazione passata non esiste");
        }
        return richiestaRepository.findByOrdinazioneId(ordinazione.getId());
    }

    public List<Ordinazione> getRichiesteWith(Stato stato){
        this.checkIsNull(stato);
        return ordinazioneRepository.findByStato(stato);
    }

    public Ordinazione creaOrdinazione(List<Richiesta> richieste, Ombrellone ombrellone){
        this.checkIsNull(richieste, ombrellone);
        Ordinazione ordinazione = new Ordinazione(ombrellone);
        if(this.checkProdotti(richieste)) return null;
        if(!this.needImpostaPrezzo(richieste)){
            ordinazione.setPrezzoTot(this.getPrezzoTotaleRichieste(richieste));
        }
        ordinazioneRepository.save(ordinazione);
        for(Richiesta richiesta: richieste){
            richiesta.setOrdinazione(ordinazione);
            gestoreProdotti.decrementoQuantitaProdotto(richiesta.getProdotto(), richiesta.getQuantita());
        }
        richiestaRepository.saveAll(richieste);
        return ordinazione;
    }

    public boolean annullaOrdinazione(Ordinazione ordinazione){
        this.checkIsNull(ordinazione);
        if(!ordinazioneRepository.existsById(ordinazione.getId())){
            return false;
        }
        for(Richiesta richiesta: richiestaRepository.findByOrdinazioneId(ordinazione.getId())){
            gestoreProdotti.incrementoQuantitaProdotto(richiesta.getProdotto(), richiesta.getQuantita());
        }
        ordinazioneRepository.deleteById(ordinazione.getId());
        return true;
    }

    public boolean impostaPrezzoRichiesta(Richiesta richiesta, double prezzo){
        this.checkIsNull(richiesta, prezzo);
        if(!richiestaRepository.existsById(richiesta.getId())){
            throw new IllegalArgumentException("I parametri passati non esistono");
        }
        richiesta.setPrezzo(prezzo);
        richiestaRepository.save(richiesta);
        return true;
    }

    public double ricalcolaPrezzoFinale(Ordinazione ordinazione){
        this.checkIsNull(ordinazione);
        if(ordinazione.getPrezzoTot() != 0){
            return ordinazione.getPrezzoTot();
        }
        return this.getPrezzoTotaleRichieste(richiestaRepository.findByOrdinazioneId(ordinazione.getId()));
    }

    public List<Richiesta> listaRichiesteConModifiche(Ordinazione ordinazione){
        List<Richiesta> richieste = new ArrayList<>();
        for(Richiesta richiesta: richiestaRepository.findByOrdinazioneId(ordinazione.getId())){
            if(!richiesta.getModifiche().isEmpty()){
                richieste.add(richiesta);
            }
        }
        return richieste;
    }

    public void setStato(Ordinazione ordinazione, Stato stato){
        this.checkIsNull(ordinazione, stato);
        if(!ordinazioneRepository.existsById(ordinazione.getId())){
            throw new IllegalArgumentException("L'ordinazione passata non esiste");
        }
        ordinazione.setStato(stato);
        ordinazioneRepository.save(ordinazione);
    }

    public boolean needImpostaPrezzo(List<Richiesta> richieste){
        this.checkIsNull(richieste);
        for(Richiesta richiesta: richieste){
            if(!richiesta.getModifiche().isEmpty()){
                return true;
            }
        }
        return false;
    }

    private boolean checkProdotti(List<Richiesta> richieste){
        for(Richiesta richiesta: richieste){
            if(!gestoreProdotti.isPresent(richiesta.getProdotto(), richiesta.getQuantita())){
                return false;
            }
        }
        return true;
    }

    private double getPrezzoTotaleRichieste(List<Richiesta> richieste){
        double prezzoFinale = 0;
        for(Richiesta richiesta: richieste){
            prezzoFinale+=richiesta.getPrezzo();
        }
        return prezzoFinale;
    }

    private void checkIsNull(Object ... objects){
        for(Object obj: objects){
            if(obj == null){
                throw new NullPointerException("I paramentri passati sono nulli");
            }
        }
    }

}