package pelicann.linnca.com.corefunctionality.lessongeneration.lessons;


import pelicann.linnca.com.corefunctionality.connectors.EndpointConnectorReturnsXML;
import pelicann.linnca.com.corefunctionality.connectors.SPARQLDocumentParserHelper;
import pelicann.linnca.com.corefunctionality.connectors.WikiBaseEndpointConnector;
import pelicann.linnca.com.corefunctionality.connectors.WikiDataSPARQLConnector;
import pelicann.linnca.com.corefunctionality.db.Database;
import pelicann.linnca.com.corefunctionality.lessondetails.LessonInstanceData;
import pelicann.linnca.com.corefunctionality.lessongeneration.FeedbackPair;
import pelicann.linnca.com.corefunctionality.lessongeneration.GrammarRules;
import pelicann.linnca.com.corefunctionality.lessongeneration.Lesson;
import pelicann.linnca.com.corefunctionality.questions.QuestionData;
import pelicann.linnca.com.corefunctionality.questions.QuestionSetData;

import pelicann.linnca.com.corefunctionality.questions.QuestionTypeMappings;
import pelicann.linnca.com.corefunctionality.questions.QuestionUniqueMarkers;
import pelicann.linnca.com.corefunctionality.userinterests.WikiDataEntity;
import pelicann.linnca.com.corefunctionality.vocabulary.VocabularyWord;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;


public class COUNTRY_possessive_government_is_a_GOVERNMENT extends Lesson {
    public static final String KEY = "COUNTRY_possessive_government_is_a_GOVERNMENT";
    private final List<QueryResult> queryResults = new ArrayList<>();
    private class QueryResult {

        private final String countryID;
        private final String countryEN;
        private final String countryJP;
        private final String governmentEN;
        private final String governmentJP;

        private QueryResult(
                String countryID,
                String countryEN,
                String countryJP,
                String governmentEN,
                String governmentJP
        ) {

            this.countryID = countryID;
            this.countryEN = countryEN;
            this.countryJP = countryJP;
            this.governmentEN = governmentEN;
            this.governmentJP = governmentJP;
        }
    }



    public COUNTRY_possessive_government_is_a_GOVERNMENT(EndpointConnectorReturnsXML connector, Database db, LessonListener listener){

        super(connector, db, listener);
        super.categoryOfQuestion = WikiDataEntity.CLASSIFICATION_PLACE;
        super.questionSetsToPopulate = 3;
        super.lessonKey = KEY;
        super.questionOrder = LessonInstanceData.QUESTION_ORDER_ORDER_BY_SET;

    }

    @Override
    protected String getSPARQLQuery(){
        return "SELECT ?country ?countryEN ?countryLabel " +
                " ?governmentEN ?governmentLabel " +
                "WHERE " +
                "{" +
                "    ?country wdt:P31 wd:Q6256 . " + //is a country
                "    ?country wdt:P122 ?government . " + //has a basic form of gov
                "    ?country rdfs:label ?countryEN . " +
                "    ?government rdfs:label ?governmentEN . " +
                "    FILTER (LANG(?countryEN) = '" +
                WikiBaseEndpointConnector.ENGLISH + "') . " +
                "    FILTER (LANG(?governmentEN) = '" +
                WikiBaseEndpointConnector.ENGLISH + "') . " +
                "    SERVICE wikibase:label { bd:serviceParam wikibase:language '" +
                WikiBaseEndpointConnector.LANGUAGE_PLACEHOLDER + "', " + //JP label if possible
                "    '" + WikiBaseEndpointConnector.ENGLISH + "'} . " + //fallback language is English
                "    BIND (wd:%s as ?country) . " + //binding the ID of entity as ?country
                "} ";

    }



    @Override

    protected synchronized void processResultsIntoClassWrappers(Document document) {

        NodeList allResults = document.getElementsByTagName(
                WikiDataSPARQLConnector.RESULT_TAG
        );

        int resultLength = allResults.getLength();

        for (int i=0; i<resultLength; i++) {
            Node head = allResults.item(i);
            String countryID = SPARQLDocumentParserHelper.findValueByNodeName(head, "country");

            countryID = WikiDataEntity.getWikiDataIDFromReturnedResult(countryID);
            String countryEN = SPARQLDocumentParserHelper.findValueByNodeName(head, "countryEN");
            String countryJP = SPARQLDocumentParserHelper.findValueByNodeName(head, "countryLabel");
            String governmentEN = SPARQLDocumentParserHelper.findValueByNodeName(head, "governmentEN");
            String governmentJP = SPARQLDocumentParserHelper.findValueByNodeName(head, "governmentLabel");
            
            QueryResult qr = new QueryResult(countryID, countryEN, countryJP, governmentEN, governmentJP);

            queryResults.add(qr);

        }

    }



    @Override

    protected synchronized int getQueryResultCt(){
        return queryResults.size();
    }

    protected synchronized void createQuestionsFromResults(){

        for (QueryResult qr : queryResults){
            List<List<QuestionData>> questionSet = new ArrayList<>();
            List<QuestionData> spellingSuggestiveQuestion = spellingQuestion(qr);
            questionSet.add(spellingSuggestiveQuestion);

            List<QuestionData> translateWordQuestion = createTranslateWordQuestion(qr);
            questionSet.add(translateWordQuestion);

            List<QuestionData> fillInBlankInput2Question = createFillInBlankInputQuestion2(qr);
            questionSet.add(fillInBlankInput2Question);

            List<QuestionData> fillInBlankInput1Question = createFillInBlankInputQuestion1(qr);
            questionSet.add(fillInBlankInput1Question);

            List<VocabularyWord> vocabularyWords = getVocabularyWords(qr);

            super.newQuestions.add(new QuestionSetData(questionSet, qr.countryID, qr.countryJP, vocabularyWords));
        }



    }

    private List<VocabularyWord> getVocabularyWords(QueryResult qr){
        VocabularyWord government = new VocabularyWord("","government", "政治体制",
                formatSentenceEN(qr), formatSentenceJP(qr), KEY);
        VocabularyWord country = new VocabularyWord("", qr.countryEN,qr.countryJP,
                formatSentenceEN(qr), formatSentenceJP(qr), KEY);
        VocabularyWord govType = new VocabularyWord("", qr.governmentEN,qr.governmentJP,
                formatSentenceEN(qr), formatSentenceJP(qr), KEY);

        List<VocabularyWord> words = new ArrayList<>(3);
        words.add(government);
        words.add(country);
        words.add(govType);
        return words;
    }

    private String formatSentenceEN(QueryResult qr){
        String sentence = GrammarRules.definiteArticleBeforeCountry(qr.countryEN) + "\'s government is " +
                GrammarRules.indefiniteArticleBeforeNoun(qr.governmentEN) + ".";
        return GrammarRules.uppercaseFirstLetterOfSentence(sentence);
    }

    private String formatSentenceJP(QueryResult qr){
        return qr.countryJP + "の政治体制は" + qr.governmentJP + "です。";
    }

    private List<QuestionData> spellingQuestion(QueryResult qr){
        String question = qr.countryJP;
        String answer = GrammarRules.definiteArticleBeforeCountry(qr.countryEN);
        QuestionData data = new QuestionData();
        data.setId("");
        data.setLessonId(super.lessonKey);

        data.setQuestionType(QuestionTypeMappings.CHOOSECORRECTSPELLING);
        data.setQuestion(question);
        data.setChoices(null);
        //for suggestive, we don't need to lowercase everything
        data.setAnswer(answer);
        data.setAcceptableAnswers(null);

        data.setFeedback(null);

        List<QuestionData> questionVariations = new ArrayList<>();
        questionVariations.add(data);
        return questionVariations;

    }

    private FeedbackPair fillInBlankInputFeedback(QueryResult qr){
        String lowercaseCountry = GrammarRules.definiteArticleBeforeCountry(qr.countryEN)
                .toLowerCase();

        String lowercaseCountryWithoutThe = qr.countryEN.toLowerCase();
        List<String> responses = new ArrayList<>(2);
        responses.add(lowercaseCountry);
        if (!lowercaseCountry.equals(lowercaseCountryWithoutThe))
            responses.add(lowercaseCountryWithoutThe);
        String feedback = "国の名前は大文字で始まります。\n" + lowercaseCountryWithoutThe + " → " +
                        GrammarRules.definiteArticleBeforeCountry(qr.countryEN);
        return new FeedbackPair(responses, feedback, FeedbackPair.EXPLICIT);
    }

    private List<QuestionData> createTranslateWordQuestion(QueryResult qr){
        String question = qr.countryJP;
        String answer = GrammarRules.definiteArticleBeforeCountry(qr.countryEN);
        List<String> acceptableAnswers = new ArrayList<>(1);
        if (!answer.equals(qr.countryEN))
            acceptableAnswers.add(qr.countryEN);
        FeedbackPair feedbackPair = fillInBlankInputFeedback(qr);
        List<FeedbackPair> feedbackPairs = new ArrayList<>();
        feedbackPairs.add(feedbackPair);
        QuestionData data = new QuestionData();
        data.setId("");
        data.setLessonId(super.lessonKey);
        data.setQuestionType(QuestionTypeMappings.TRANSLATEWORD);
        data.setQuestion(question);
        data.setChoices(null);
        data.setAnswer(answer);
        data.setAcceptableAnswers(acceptableAnswers);
        data.setFeedback(feedbackPairs);
        List<QuestionData> questionDataList = new ArrayList<>();
        questionDataList.add(data);

        return questionDataList;
    }

    private String fillInBlankInputQuestion2(QueryResult qr){
        String sentence1 = qr.countryJP + "の政治体制は" + qr.governmentJP + "です。";
        String government = GrammarRules.indefiniteArticleBeforeNoun(qr.governmentEN);
        String sentence2 = QuestionUniqueMarkers.FILL_IN_BLANK_INPUT_TEXT +
                " government is " + government + ".";
        return sentence1 + "\n\n" + sentence2;
    }



    private String fillInBlankInputAnswer2(QueryResult qr){
        return GrammarRules.definiteArticleBeforeCountry(qr.countryEN) + "'s";
    }

    private List<String> fillInBlankInputAlternateAnswer2(QueryResult qr){
        List<String> alternateAnswers = new ArrayList<>(1);
        String definiteArticleString = GrammarRules.definiteArticleBeforeCountry(qr.countryEN);
        if (!definiteArticleString.equals(qr.countryEN)){
            alternateAnswers.add(qr.countryEN);
        }

        return alternateAnswers;
    }

    private List<QuestionData> createFillInBlankInputQuestion2(QueryResult qr){
        String question = this.fillInBlankInputQuestion2(qr);
        String answer = fillInBlankInputAnswer2(qr);
        List<String> acceptableAnswers = fillInBlankInputAlternateAnswer2(qr);
        QuestionData data = new QuestionData();
        data.setId("");
        data.setLessonId(super.lessonKey);

        data.setQuestionType(QuestionTypeMappings.FILLINBLANK_INPUT);
        data.setQuestion(question);
        data.setChoices(null);
        data.setAnswer(answer);
        data.setAcceptableAnswers(acceptableAnswers);

        data.setFeedback(null);

        List<QuestionData> questionDataList = new ArrayList<>();
        questionDataList.add(data);
        return questionDataList;
    }

    private String fillInBlankInputQuestion1(QueryResult qr){
        String sentence1 = formatSentenceJP(qr);
        String government = GrammarRules.indefiniteArticleBeforeNoun(qr.governmentEN);
        String sentence2 = QuestionUniqueMarkers.FILL_IN_BLANK_INPUT_TEXT +
                " is " + government + ".";
        return sentence1 + "\n\n" + GrammarRules.uppercaseFirstLetterOfSentence(sentence2);
    }

    private String fillInBlankInputAnswer1(QueryResult qr){
        return GrammarRules.definiteArticleBeforeCountry(qr.countryEN) + "'s government";
    }

    private List<String> fillInBlankInputAcceptableAnswers(QueryResult qr){
        List<String> answers = new ArrayList<>(1);
        if (!GrammarRules.definiteArticleBeforeCountry(qr.countryEN)
                .equals(qr.countryEN))
            answers.add(qr.countryEN + "'s government");
        return answers;
    }

    private List<QuestionData> createFillInBlankInputQuestion1(QueryResult qr){
        String question = this.fillInBlankInputQuestion1(qr);
        String answer = fillInBlankInputAnswer1(qr);
        List<String> acceptableAnswers = fillInBlankInputAcceptableAnswers(qr);
        QuestionData data = new QuestionData();
        data.setId("");
        data.setLessonId(super.lessonKey);
        data.setQuestionType(QuestionTypeMappings.FILLINBLANK_INPUT);
        data.setQuestion(question);
        data.setChoices(null);
        data.setAnswer(answer);
        data.setAcceptableAnswers(acceptableAnswers);
        data.setFeedback(null);
        List<QuestionData> questionDataList = new ArrayList<>();
        questionDataList.add(data);
        return questionDataList;

    }

    private List<QuestionData> spellingSuggestiveQuestionGeneric(){
        String question = "政治体制";
        String answer = "government";
        QuestionData data = new QuestionData();
        data.setId("");
        data.setLessonId(super.lessonKey);

        data.setQuestionType(QuestionTypeMappings.SPELLING_SUGGESTIVE);
        data.setQuestion(question);
        data.setChoices(null);
        //for suggestive, we don't need to lowercase everything
        data.setAnswer(answer);
        data.setAcceptableAnswers(null);

        data.setFeedback(null);

        List<QuestionData> questionVariations = new ArrayList<>();
        questionVariations.add(data);
        return questionVariations;

    }

    @Override
    protected List<List<QuestionData>> getPreGenericQuestions(){
        List<List<QuestionData>> questionSet = new ArrayList<>(1);
        List<QuestionData> spellingSuggestiveQuestion = spellingSuggestiveQuestionGeneric();
        questionSet.add(spellingSuggestiveQuestion);
        return questionSet;

    }

}