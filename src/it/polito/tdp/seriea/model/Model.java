package it.polito.tdp.seriea.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;

import it.polito.tdp.seriea.db.SerieADAO;

public class Model {
	
	private Team squadra;
	
	private List<Team> squadre;
	private List<Season> stagioni;
	private List<Season> stagioniConsecutive;
	
	private Map<Season, Integer> punteggi;
	
	private Map<Integer, Season> stagioniIdMap;
	private Map<String, Team> squadreIdMap;
	
	private Graph<Season, DefaultWeightedEdge> grafo;

	private List<Season> best;
	
	public Model() {
		SerieADAO dao = new SerieADAO();
		
		this.squadre = dao.listTeams();
		this.stagioni = dao.listAllSeasons();
		
		this.stagioniIdMap = new HashMap<>();
		this.squadreIdMap = new HashMap<>();
		
		for(Season s : this.stagioni) {
			stagioniIdMap.put(s.getSeason(), s);
		}
		
		for(Team t : this.squadre) {
			squadreIdMap.put(t.getTeam(), t);
		}
		
	}
	
	public List<Team> getSquadre(){
		return squadre;
	}
	
	public Map<Season, Integer> calcolaPunteggi(Team squadra) {
		
		this.squadra = squadra;
		this.punteggi = new HashMap<>();
		
		SerieADAO dao = new SerieADAO();
		List<Match> partite = dao.listMatchesForTeam(squadra, stagioniIdMap, squadreIdMap);
		
		for(Match m : partite) {
			
			Season stagione = m.getSeason();
			
			int punti = 0;
			
			if(m.getFtr().equals("D")) {
				punti = 1;
			}
			
			else {
				if((m.getHomeTeam().equals(squadra) && m.getFtr().equals("H")) || (m.getAwayTeam().equals(squadra) && m.getFtr().equals("A"))) {
					punti = 3;
				}
			}
			
			Integer attuale = punteggi.get(stagione);
			if(attuale == null)
				attuale = 0;
			punteggi.put(stagione, attuale+punti);
		}
		
		return punteggi;
	}
	
	public Season calcolaAnnataDOro() {
		
		grafo = new SimpleDirectedWeightedGraph<>(DefaultWeightedEdge.class);
		
		// vertici
		
		Graphs.addAllVertices(grafo, punteggi.keySet());
		
		// archi
		
		for(Season s1 : punteggi.keySet()) {
			for(Season s2 : punteggi.keySet()) {
				if(!s1.equals(s2)) {
					int punti1 = punteggi.get(s1);
					int punti2 = punteggi.get(s2);
					
					if(punti1 > punti2) {
						Graphs.addEdge(grafo, s2, s1, punti1-punti2);
					}
					else {
						Graphs.addEdge(grafo, s1, s2, punti2-punti1);
					}
				}
			}
		}
		
		// annata migliore
		
		Season best = null;
		int max = 0;
		for(Season s : grafo.vertexSet()) {
			int valore = pesoStagione(s);
			if(valore > max) {
				max = valore;
				best = s;
			}
		}
		
		return best;
	}
	
	private int pesoStagione(Season s) {
		int somma = 0;
		
		for(DefaultWeightedEdge e : grafo.incomingEdgesOf(s)) {
			somma += grafo.getEdgeWeight(e);
		}
		
		for(DefaultWeightedEdge e : grafo.outgoingEdgesOf(s)) {
			somma -= grafo.getEdgeWeight(e);
		}
		
		return somma;

	}
	
	public List<Season> camminoVirtuoso() {
		
		// trova stagioni consecutive
		this.stagioniConsecutive = new ArrayList<Season>(punteggi.keySet());
		Collections.sort(stagioniConsecutive);
		
		// inizializzazione variabili utili alla ricorsione
		List<Season> parziale = new ArrayList<Season>();
		this.best = new ArrayList<>();
		
		// Iterazione del livello 0 della ricorsione
		for(Season s : grafo.vertexSet()) {
			parziale.add(s);
			cerca(1, parziale);
			parziale.remove(0);
		}
		
		return best;

	}
		
		
	/*
	 * RICORSIONE
	 * 
	 * Soluzione parziale: lista di season (vertici)
	 * Livello: lunghezza della lista
	 * Casi terminali: non ho altri vertici da aggiungere (non c'è nessuna nuova stagione)
	 * 
	 * --> verifico se il cammino trovato è il migliore tra quelli analizzati e nel caso aggiorno
	 * 
	 * Genero le soluzioni parziali per il livello successivo: vertici connessi all'ultimo vertice del percorso
	 * (con arco orientato nel verso giusto), non ancora parte del percorso, relativi a stagioni consecutive
	 */
		
	
	private void cerca(int livello, List<Season> parziale) {
		
		boolean trovato = false;
		
		// genero nuove soluzioni
		Season ultimo = parziale.get(livello-1);
		
		for(Season prossimo : Graphs.successorListOf(grafo, ultimo)) {
			if(!parziale.contains(prossimo)) {
				if(stagioniConsecutive.indexOf(ultimo)+1 == stagioniConsecutive.lastIndexOf(prossimo)){
					
					// candidato accettabile, via con la ricorsione
					trovato = true;
					parziale.add(prossimo);
					cerca(livello+1, parziale);
					
					parziale.remove(livello);
				}
			}
		}
		
		// valuto se mi trovo nel caso terminale
		
		if(!trovato) {
			if(parziale.size() > best.size())
				
				/*
				 *  DEVO SALVARE UN CLONE!!!!!
				 */
				
				best = new ArrayList<Season>(parziale);
		}
	}
	
	
}
