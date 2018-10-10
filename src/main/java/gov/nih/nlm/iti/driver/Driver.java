package gov.nih.nlm.iti.driver;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import gov.nih.nlm.iti.text.main.NxmlParser;
import gov.nih.nlm.iti.text.main.PmcFigureMention;
import gov.nih.nlm.iti.text.main.PmcFigureRecord;
import gov.nih.nlm.iti.text.main.PmcFull;
import gov.nih.nlm.iti.text.model.Document;
import gov.nih.nlm.iti.text.model.Figure;
import gov.nih.nlm.iti.text.model.FigureMention;
import gov.nih.nlm.iti.text.model.Journal;
import gov.nih.nlm.iti.text.model.PublicationDate;
import gov.nih.nlm.iti.text.nxml.TextRange;
import org.novasearch.medical.medannotator.MedAnnotatorAux;

/*
 * Example class to demonstrate the usage of the NXML Parser
 */
public class Driver {

	public static final String DATA_PATH = "/home/amourao/code/jmedical/iclefj/data/mesh/";

	public static void main(String args[]) throws IOException {
		// Provide the path to NXML file on disk.
		String inputFileName = "/home/amourao/code/jmedical/OpenI-NxmlParser/nxml/3232562.nxml";
		inputFileName = "/home/amourao/Desktop/1743-422X-9-248.nxml";
		inputFileName = "/media/Share/2018-04_sample_pubmedlists_extracted/02/a8/PMC4458552/CRIPE2015-938264.nxml";
		
		File inputFile = new File(inputFileName);

		NxmlParser nxml = new NxmlParser();
		PmcFull articleMeta = new PmcFull();
		articleMeta.setLocalFileName(inputFile.getName());

		LinkedList<PmcFigureRecord> recordList = new LinkedList<PmcFigureRecord>();
		LinkedList<PmcFigureMention> mentionList = new LinkedList<PmcFigureMention>();

		try {
			nxml.getPmcFields(inputFile.getPath(), recordList, articleMeta,
					mentionList);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			System.out
					.println("Error parsing the input NXML: " + inputFileName);
			e.printStackTrace();
			System.exit(0);
		}

		// Create a unique set of figure mentions.
		HashMap<String, List<FigureMention>> figureIdToMention = new HashMap<String, List<FigureMention>>();
		for (int i = 0; i < mentionList.size(); i++) {
			PmcFigureMention pmcFigureMention = mentionList.get(i);
			FigureMention figureMention = new FigureMention();
			figureMention.setParagraph(pmcFigureMention.getParagraph()
					.toString());
			String figureId = pmcFigureMention.getFigureId().trim();

			if (!figureIdToMention.containsKey(figureId)) {
				figureIdToMention.put(figureId, new ArrayList<FigureMention>());
			}

			List<FigureMention> figureMentions = figureIdToMention
					.get(figureId);

			if (!figureMentions.contains(figureMention)) {
				figureMentions.add(figureMention);
			}
		}

		// Create a Document object and populate it with extracted data.
		Document document = new Document();
		document.setArticleDOI(articleMeta.getArticleDOI());
		document.setArticleURL(articleMeta.getArticleURL());
		document.setLocalFileName(inputFile.getAbsolutePath());
		document.setTitle(articleMeta.getTitle().trim());
		document.setPmcid(articleMeta.getPmcid());
		document.setPmid(articleMeta.getPmid());
		document.setPublisherName(articleMeta.getPublisherName());
		document.setLicenseTypes(articleMeta.getLicenseTypes());
		document.setLicenseURLs(articleMeta.getLicenseURLs());
		document.setAbstractText(articleMeta.getAbstractText());
		document.setCategories(articleMeta.getSubjects());
		document.setKeywords(articleMeta.getKeywords());
		document.setAuthors(articleMeta.getAuthors());
		// Populate Journal info if the data is available.
		if (articleMeta.getJournalId() != null
				|| articleMeta.getJournalIssn() != null
				|| articleMeta.getJournalTitle() != null) {
			Journal journal = new Journal();
			journal.setId(articleMeta.getJournalId());
			journal.setIssn(articleMeta.getJournalIssn());
			journal.setTitle(articleMeta.getJournalTitle());
			document.setJournal(journal);
		}

		// Populate the date field if data is available.
		if ((articleMeta.getDay() != null && !articleMeta.getDay().trim()
				.equals(""))
				|| (articleMeta.getMonth() != null && !articleMeta.getMonth()
						.trim().equals(""))
				|| (articleMeta.getYear() != null && !articleMeta.getYear()
						.trim().equals(""))) {
			PublicationDate publicationDate = new PublicationDate();
			document.setPublicationDate(publicationDate);
			publicationDate.setDate(articleMeta.getDay());
			publicationDate.setMonth(articleMeta.getMonth());
			publicationDate.setYear(articleMeta.getYear());
			publicationDate.setType("epub");
		}

		// Link the parsed images with captions and mentions.
		LinkedList<Figure> figures = new LinkedList<Figure>();
		for (int i = 0; i < recordList.size(); i++) {

			Figure figure = new Figure();

			figures.add(figure);
			PmcFigureRecord pmcFigureRecord = recordList.get(i);

			figure.setFigureID(pmcFigureRecord.getFigureID().trim());
			figure.setIriList(pmcFigureRecord.getIriList());
			figure.setIriVideoMap(pmcFigureRecord.getIriVideoMap());
			figure.setLabel(pmcFigureRecord.getLabel().trim());

			LinkedList<TextRange> textRanges = nxml
					.getBoldAndItalicTextRanges(pmcFigureRecord);
			String caption = "";
			if (pmcFigureRecord.getCaption() != null) {
				caption = pmcFigureRecord.getCaption().trim().replace("\n", " ");
			}
			figure.setCaption(caption);

			List<FigureMention> figureMentions = figureIdToMention.get(figure
					.getFigureID());

			if (figureMentions == null) {
				figureMentions = new ArrayList<FigureMention>();
			}
			figure.setFigureMentions(figureMentions);
		}
		// Add figures to the Document object.
		document.setFigures(figures);

		// Print article info.
		System.out
				.println("-------------------- Article Metadata --------------------");
		System.out.println("Article PMCID: " + document.getPmcid());
		System.out.println("Article PMID: " + document.getPmid());
		System.out.println("Article Title: " + document.getTitle());
		System.out.println("Article Pub. date: "
				+ document.getPublicationDate());
		System.out.println("Article authors: " + articleMeta.getAuthors());

		System.out.print("Article Categories: ");
		for (String cat : document.getCategories())
			System.out.print(cat + "; ");
		System.out.println();

		System.out.print("Article Keywords: ");
		for (String cat : document.getKeywords())
			System.out.print(cat + "; ");
		System.out.println();

		System.out.println("Article Title: " + document.getTitle());
		System.out.println("Article Authors: " + document.getAuthors());
		if (document.getJournal() != null) {
			System.out.println("Article Journal ID: "
					+ document.getJournal().getId());
			System.out.println("Article Journal ISSN: "
					+ document.getJournal().getIssn());
			System.out.println("Article Journal Title: "
					+ document.getJournal().getTitle());
		}
		System.out.println("Article Publisher: " + document.getPublisherName());
		System.out.println("Article Abstract: " + document.getAbstractText());
		System.out
				.println("Number of Figures: " + document.getFigures().size());
		System.out.println("");

		// Print article image info.
		if (document.getFigures().size() > 0) {
			System.out
					.println("-------------------- Figure List --------------------");
		}
		for (Figure figure : document.getFigures()) {
			if (!figure.getIriList().isEmpty()) {
				System.out.println("Figure ID: " + figure.getFigureID());
				System.out.println("Figure Label: " + figure.getLabel());
				System.out.println("Figure Identifier: "
						+ figure.getIriList().get(0));
				System.out.println("Figure Caption: " + figure.getCaption());
				StringBuilder mentionBuilder = new StringBuilder();
				for (FigureMention mention : figure.getFigureMentions()) {
					if (mentionBuilder.length() == 0) {
						mentionBuilder.append(mention.getParagraph());
					} else {
						mentionBuilder.append(" " + mention.getParagraph());
					}
				}
				System.out.println("Figure Mention: "
						+ mentionBuilder.toString());
				System.out.println("----------");
				System.out.println("");
			}
		}

		System.out
				.println("-------------------- Article Full Text --------------------");
		System.out.println(articleMeta.getFullText());

		HashSet<String> demoSet = MedAnnotatorAux.load(DATA_PATH
				+ "demographics.txt");
		HashSet<String> placeSet = MedAnnotatorAux.load(DATA_PATH
				+ "place_names.txt");
		HashSet<String> ageSet_suff = MedAnnotatorAux.loadAge(DATA_PATH
				+ "age_suffix.txt");
		HashSet<String> ageSet_pref = MedAnnotatorAux.loadAge(DATA_PATH
				+ "age_prefix.txt");

		HashMap<String, String> exp = MedAnnotatorAux
				.loadExceptionList(DATA_PATH + "demographics_prefered.txt");

		HashSet<String> C = MedAnnotatorAux.load(DATA_PATH + "C.txt");
		// HashSet<String> D =
		// MedAnnotatorAux.load("/home/amourao/code/jmedical/iclefj/data/mesh/D_base.txt");
		HashSet<String> E = MedAnnotatorAux.load(DATA_PATH + "E.txt");

		HashSet<String> megaSet = new HashSet<String>();
		megaSet.addAll(C);
		megaSet.addAll(E);

		String abstractText = document.getTitle() + " " + document.getAbstractText();
		abstractText = abstractText.replaceAll("\\W ", " ");
		Set<String> demotags = MedAnnotatorAux.getTotalWords(abstractText,
				demoSet, exp);
		Set<String> placetags = MedAnnotatorAux.getTotalWords(abstractText,
				placeSet, new HashMap<String, String>());
		List<Integer> agetags = MedAnnotatorAux.getNumericalAge(abstractText,
				ageSet_suff, ageSet_pref);
		Set<String> meshtags = MedAnnotatorAux.getTotalWords(abstractText,
				megaSet, new HashMap<String, String>());

		System.out.println(demotags);
		System.out.println(placetags);
		System.out.println(agetags);
		System.out.println(meshtags);

	}

	public static PmcFull getInfo(String inputFile) {
		// Provide the path to NXML file on disk.

		NxmlParser nxml = new NxmlParser();
		PmcFull articleMeta = new PmcFull();
		articleMeta.setLocalFileName(inputFile);

		LinkedList<PmcFigureRecord> recordList = new LinkedList<PmcFigureRecord>();
		LinkedList<PmcFigureMention> mentionList = new LinkedList<PmcFigureMention>();

		try {
			nxml.getPmcFields(inputFile, recordList, articleMeta, mentionList);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			System.out.println("Error parsing the input NXML: " + inputFile);
			e.printStackTrace();
			// System.exit(0);
		}

		// Create a unique set of figure mentions.
		HashMap<String, List<FigureMention>> figureIdToMention = new HashMap<String, List<FigureMention>>();
		for (int i = 0; i < mentionList.size(); i++) {
			PmcFigureMention pmcFigureMention = mentionList.get(i);
			FigureMention figureMention = new FigureMention();
			figureMention.setParagraph(pmcFigureMention.getParagraph()
					.toString());
			String figureId = pmcFigureMention.getFigureId().trim();

			if (!figureIdToMention.containsKey(figureId)) {
				figureIdToMention.put(figureId, new ArrayList<FigureMention>());
			}

			List<FigureMention> figureMentions = figureIdToMention
					.get(figureId);

			if (!figureMentions.contains(figureMention)) {
				figureMentions.add(figureMention);
			}
		}

		// Create a Document object and populate it with extracted data.
		Document document = new Document();
		document.setArticleDOI(articleMeta.getArticleDOI());
		document.setArticleURL(articleMeta.getArticleURL());
		document.setLocalFileName(inputFile);
		document.setTitle(articleMeta.getTitle().trim());
		document.setPmcid(articleMeta.getPmcid());
		document.setPmid(articleMeta.getPmid());
		document.setPublisherName(articleMeta.getPublisherName());
		document.setLicenseTypes(articleMeta.getLicenseTypes());
		document.setLicenseURLs(articleMeta.getLicenseURLs());
		document.setAbstractText(articleMeta.getAbstractText());

		// Populate Journal info if the data is available.
		if (articleMeta.getJournalId() != null
				|| articleMeta.getJournalIssn() != null
				|| articleMeta.getJournalTitle() != null) {
			Journal journal = new Journal();
			journal.setId(articleMeta.getJournalId());
			journal.setIssn(articleMeta.getJournalIssn());
			journal.setTitle(articleMeta.getJournalTitle());
			document.setJournal(journal);
		}

		// Populate the date field if data is available.
		if ((articleMeta.getDay() != null && !articleMeta.getDay().trim()
				.equals(""))
				|| (articleMeta.getMonth() != null && !articleMeta.getMonth()
						.trim().equals(""))
				|| (articleMeta.getYear() != null && !articleMeta.getYear()
						.trim().equals(""))) {
			PublicationDate publicationDate = new PublicationDate();
			document.setPublicationDate(publicationDate);
			publicationDate.setDate(articleMeta.getDay());
			publicationDate.setMonth(articleMeta.getMonth());
			publicationDate.setYear(articleMeta.getYear());
			publicationDate.setType("epub");
		}

		// Link the parsed images with captions and mentions.
		LinkedList<Figure> figures = new LinkedList<Figure>();
		for (int i = 0; i < recordList.size(); i++) {

			Figure figure = new Figure();

			figures.add(figure);
			PmcFigureRecord pmcFigureRecord = recordList.get(i);

			figure.setFigureID(pmcFigureRecord.getFigureID().trim());
			figure.setIriList(pmcFigureRecord.getIriList());
			figure.setIriVideoMap(pmcFigureRecord.getIriVideoMap());
			figure.setLabel(pmcFigureRecord.getLabel().trim());

			LinkedList<TextRange> textRanges = nxml
					.getBoldAndItalicTextRanges(pmcFigureRecord);
			String caption = "";
			if (pmcFigureRecord.getCaption() != null) {
				caption = pmcFigureRecord.getCaption().trim().replace("\n", " ");
			}
			figure.setCaption(caption);

			List<FigureMention> figureMentions = figureIdToMention.get(figure
					.getFigureID());

			if (figureMentions == null) {
				figureMentions = new ArrayList<FigureMention>();
			}
			figure.setFigureMentions(figureMentions);
		}
		// Add figures to the Document object.
		document.setFigures(figures);

		return articleMeta;

	}

	public static Document getDocInfo(String inputFile) {
		// Provide the path to NXML file on disk.

		NxmlParser nxml = new NxmlParser();
		PmcFull articleMeta = new PmcFull();
		articleMeta.setLocalFileName(inputFile);

		LinkedList<PmcFigureRecord> recordList = new LinkedList<PmcFigureRecord>();
		LinkedList<PmcFigureMention> mentionList = new LinkedList<PmcFigureMention>();

		try {
			nxml.getPmcFields(inputFile, recordList, articleMeta, mentionList);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			System.out.println("Error parsing the input NXML: " + inputFile);
			e.printStackTrace();
			// System.exit(0);
		}

		// Create a unique set of figure mentions.
		HashMap<String, List<FigureMention>> figureIdToMention = new HashMap<String, List<FigureMention>>();
		for (int i = 0; i < mentionList.size(); i++) {
			PmcFigureMention pmcFigureMention = mentionList.get(i);
			FigureMention figureMention = new FigureMention();
			figureMention.setParagraph(pmcFigureMention.getParagraph()
					.toString());
			String figureId = pmcFigureMention.getFigureId().trim();

			if (!figureIdToMention.containsKey(figureId)) {
				figureIdToMention.put(figureId, new ArrayList<FigureMention>());
			}

			List<FigureMention> figureMentions = figureIdToMention
					.get(figureId);

			if (!figureMentions.contains(figureMention)) {
				figureMentions.add(figureMention);
			}
		}

		// Create a Document object and populate it with extracted data.
		Document document = new Document();
		document.setArticleDOI(articleMeta.getArticleDOI());
		document.setArticleURL(articleMeta.getArticleURL());
		document.setLocalFileName(inputFile);
		document.setTitle(articleMeta.getTitle().trim());
		document.setPmcid(articleMeta.getPmcid());
		document.setPmid(articleMeta.getPmid());
		document.setPublisherName(articleMeta.getPublisherName());
		document.setLicenseTypes(articleMeta.getLicenseTypes());
		document.setLicenseURLs(articleMeta.getLicenseURLs());
		document.setAbstractText(articleMeta.getAbstractText());
		document.setCategories(articleMeta.getSubjects());
		document.setKeywords(articleMeta.getKeywords());
		document.setFullText(articleMeta.getFullText());
		document.setAuthors(articleMeta.getAuthors());
		// Populate Journal info if the data is available.
		if (articleMeta.getJournalId() != null
				|| articleMeta.getJournalIssn() != null
				|| articleMeta.getJournalTitle() != null) {
			Journal journal = new Journal();
			journal.setId(articleMeta.getJournalId());
			journal.setIssn(articleMeta.getJournalIssn());
			journal.setTitle(articleMeta.getJournalTitle());
			document.setJournal(journal);
		}

		// Populate the date field if data is available.
		if ((articleMeta.getDay() != null && !articleMeta.getDay().trim()
				.equals(""))
				|| (articleMeta.getMonth() != null && !articleMeta.getMonth()
						.trim().equals(""))
				|| (articleMeta.getYear() != null && !articleMeta.getYear()
						.trim().equals(""))) {
			PublicationDate publicationDate = new PublicationDate();
			document.setPublicationDate(publicationDate);
			publicationDate.setDate(articleMeta.getDay());
			publicationDate.setMonth(articleMeta.getMonth());
			publicationDate.setYear(articleMeta.getYear());
			publicationDate.setType("epub");
		}

		// Link the parsed images with captions and mentions.
		LinkedList<Figure> figures = new LinkedList<Figure>();
		for (int i = 0; i < recordList.size(); i++) {

			Figure figure = new Figure();

			figures.add(figure);
			PmcFigureRecord pmcFigureRecord = recordList.get(i);

			figure.setFigureID(pmcFigureRecord.getFigureID().trim());
			figure.setIriList(pmcFigureRecord.getIriList());
			figure.setIriVideoMap(pmcFigureRecord.getIriVideoMap());
			figure.setLabel(pmcFigureRecord.getLabel().trim());

			LinkedList<TextRange> textRanges = nxml
					.getBoldAndItalicTextRanges(pmcFigureRecord);
			String caption = "";
			if (pmcFigureRecord.getCaption() != null) {
				caption = pmcFigureRecord.getCaption().trim().replace("\n", " ");
			}
			figure.setCaption(caption);

			List<FigureMention> figureMentions = figureIdToMention.get(figure
					.getFigureID());

			if (figureMentions == null) {
				figureMentions = new ArrayList<FigureMention>();
			}
			figure.setFigureMentions(figureMentions);
		}
		// Add figures to the Document object.
		document.setFigures(figures);

		return document;

	}
}
