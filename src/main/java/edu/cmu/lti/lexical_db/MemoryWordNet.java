package edu.cmu.lti.lexical_db;

import edu.cmu.lti.jawjaw.pobj.POS;
import edu.cmu.lti.jawjaw.util.Configuration;
import edu.cmu.lti.lexical_db.data.Concept;
import org.jdbi.v3.core.Jdbi;

import java.util.*;
import java.util.stream.Collectors;

public class MemoryWordNet implements ILexicalDatabase {

	private Map<String,List<String>> lemmaAndPosToSynsets;
	private Map<String,List<String>> hypeMap;

	public MemoryWordNet(){
		String DB = "jdbc:sqlite::resource:" + Configuration.getInstance().getWordnet();
		Jdbi jdbi = Jdbi.create(DB);
		lemmaAndPosToSynsets = new HashMap<>();
		jdbi.useHandle(handle -> {
			handle.createQuery("select lemma, pos, group_concat(synset||\"|\"||freq) as synsets from word inner join sense on sense.wordid = word.wordid where word.lang = \"eng\" group by lemma, pos order by freq desc")
					.mapToBean(Word.class)
					.forEach(word -> {
						List<String> bestSynset = Arrays.stream(word.synsets.split(",")).map(s -> s.split("\\|")[0]).collect(Collectors.toList());
						lemmaAndPosToSynsets.put(word.lemma+"|"+word.pos, bestSynset);
					});
		});

		hypeMap = new HashMap<>();
		String query = "select synset as child, group_concat(distinct synset2) as parents from sense inner join synlink on sense.synset = synlink.synset1 where link = \"hype\" and lang = \"eng\" group by synset";
		jdbi.useHandle(handle -> {
			handle.createQuery(query)
					.mapToBean(Hype.class)
					.forEach(hype -> {
						hypeMap.put(hype.child, Arrays.asList(hype.parents.split(",")));
					});
		});

	}
	
	public Collection<Concept> getAllConcepts(String word, String posText) {
		POS pos = POS.valueOf(posText);
		String key = word+"|"+posText;
		List<String> synsets = lemmaAndPosToSynsets.get(key);
		if(synsets == null)
			return new ArrayList<>();
		List<Concept> synsetStrings = new ArrayList<>(synsets.size());
		for ( String synset : synsets ) {
			synsetStrings.add(new Concept(synset, POS.valueOf(pos.toString())));
		}
		return synsetStrings;
	}

	public Collection<String> getHypernyms(String synset) {
		List<String> hypes = hypeMap.get(synset);
		return hypes == null ? new ArrayList<>() : hypes;
	}

	public Concept getMostFrequentConcept(String word, String pos) {
		Collection<Concept> concepts = getAllConcepts(word,pos);
		return concepts.size()>0 ? concepts.iterator().next():null;
	}

	public Concept findSynsetBySynset(String synset) {
		// TODO Auto-generated method stub
		return null;
	}

	// offset looks like "service#n#3"
	public String conceptToString(String synset) {
		// TODO Auto-generated method stub
		return null;
	}

	public List<String> getGloss(Concept synset, String linkString) {
		return null;
	}
	


	public static class Word {
		private String lemma, pos, synsets;

		public String getLemma() {
			return lemma;
		}

		public void setLemma(String lemma) {
			this.lemma = lemma;
		}

		public String getPos() {
			return pos;
		}

		public void setPos(String pos) {
			this.pos = pos;
		}

		public String getSynsets() {
			return synsets;
		}

		public void setSynsets(String synsets) {
			this.synsets = synsets;
		}
	}
	public static class Hype {
		private String child;
		private String parents;

		public String getChild() {
			return child;
		}

		public void setChild(String child) {
			this.child = child;
		}

		public String getParents() {
			return parents;
		}

		public void setParents(String parents) {
			this.parents = parents;
		}
	}
	
}
