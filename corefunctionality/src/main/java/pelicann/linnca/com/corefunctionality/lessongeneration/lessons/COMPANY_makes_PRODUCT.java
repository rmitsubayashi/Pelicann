package pelicann.linnca.com.corefunctionality.lessongeneration.lessons;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;

import pelicann.linnca.com.corefunctionality.connectors.EndpointConnectorReturnsXML;
import pelicann.linnca.com.corefunctionality.connectors.SPARQLDocumentParserHelper;
import pelicann.linnca.com.corefunctionality.connectors.WikiBaseEndpointConnector;
import pelicann.linnca.com.corefunctionality.connectors.WikiDataSPARQLConnector;
import pelicann.linnca.com.corefunctionality.db.Database;
import pelicann.linnca.com.corefunctionality.lessondetails.LessonInstanceData;
import pelicann.linnca.com.corefunctionality.lessongeneration.GrammarRules;
import pelicann.linnca.com.corefunctionality.lessongeneration.Lesson;
import pelicann.linnca.com.corefunctionality.questions.QuestionData;
import pelicann.linnca.com.corefunctionality.questions.QuestionSerializer;
import pelicann.linnca.com.corefunctionality.questions.QuestionSetData;
import pelicann.linnca.com.corefunctionality.questions.QuestionTypeMappings;
import pelicann.linnca.com.corefunctionality.questions.QuestionUniqueMarkers;
import pelicann.linnca.com.corefunctionality.userinterests.WikiDataEntity;
import pelicann.linnca.com.corefunctionality.vocabulary.VocabularyWord;

public class COMPANY_makes_PRODUCT extends Lesson{
    public static final String KEY = "COMPANY_makes_PRODUCT";

    private final List<QueryResult> queryResults = new ArrayList<>();
    private class QueryResult {
        private final String companyID;
        private final String companyEN;
        private final String companyJP;
        private final String productEN;
        private final String productJP;

        private QueryResult(
                String companyID,
                String companyEN,
                String companyJP,
                String productEN,
                String productJP)
        {
            this.companyID = companyID;
            this.companyEN = companyEN;
            this.companyJP = companyJP;
            this.productEN = productEN;
            this.productJP = productJP;
        }
    }

    public COMPANY_makes_PRODUCT(EndpointConnectorReturnsXML connector, Database db, LessonListener listener){
        super(connector, db, listener);
        super.questionSetsToPopulate = 1;
        super.categoryOfQuestion = WikiDataEntity.CLASSIFICATION_OTHER;
        super.lessonKey = KEY;
        super.questionOrder = LessonInstanceData.QUESTION_ORDER_ORDER_BY_SET;

    }

    @Override
    protected String getSPARQLQuery(){
        //find company name and product
        return "SELECT ?company ?companyLabel ?companyEN " +
                " ?productEN ?productLabel " +
                "WHERE " +
                "{" +
                "    ?company wdt:P31 wd:Q4830453 . " + //is a business enterprise
                "    ?company wdt:P1056 ?product . " + //makes a product/material
                "    ?company rdfs:label ?companyEN . " +
                "    ?product rdfs:label ?productEN . " +
                "    FILTER (LANG(?companyEN) = '" +
                WikiBaseEndpointConnector.ENGLISH + "') . " +
                "    FILTER (LANG(?productEN) = '" +
                WikiBaseEndpointConnector.ENGLISH + "') . " +
                "    SERVICE wikibase:label { bd:serviceParam wikibase:language '" +
                WikiBaseEndpointConnector.LANGUAGE_PLACEHOLDER + "', '" + //JP label if possible
                WikiBaseEndpointConnector.ENGLISH + "'} . " + //fallback language is English
                "    BIND (wd:%s as ?company) . " + //binding the ID of entity as ?company
                "} ";

    }

    @Override
    protected synchronized void processResultsIntoClassWrappers(Document document) {
        NodeList allResults = document.getElementsByTagName(
                WikiDataSPARQLConnector.RESULT_TAG
        );
        int resultLength = allResults.getLength();
        for (int i=0; i<resultLength; i++){
            Node head = allResults.item(i);
            String companyID = SPARQLDocumentParserHelper.findValueByNodeName(head, "company");
            companyID = WikiDataEntity.getWikiDataIDFromReturnedResult(companyID);
            String companyEN = SPARQLDocumentParserHelper.findValueByNodeName(head, "companyEN");
            String companyJP = SPARQLDocumentParserHelper.findValueByNodeName(head, "companyLabel");
            String productEN = SPARQLDocumentParserHelper.findValueByNodeName(head, "productEN");
            String productJP = SPARQLDocumentParserHelper.findValueByNodeName(head, "productLabel");

            QueryResult qr = new QueryResult(companyID, companyEN, companyJP, productEN, productJP);
            queryResults.add(qr);
        }
    }

    @Override
    protected synchronized int getQueryResultCt(){ return queryResults.size(); }

    @Override
    protected synchronized void createQuestionsFromResults(){
        for (QueryResult qr : queryResults){
            List<List<QuestionData>> questionSet = new ArrayList<>();
            List<QuestionData> sentencePuzzleQuestion = createSentencePuzzleQuestion(qr);
            questionSet.add(sentencePuzzleQuestion);

            List<QuestionData> spellingQuestion = createSpellingQuestion(qr);
            questionSet.add(spellingQuestion);

            List<QuestionData> translateQuestion = createTranslateQuestion(qr);
            questionSet.add(translateQuestion);

            List<QuestionData> fillInBlankQuestion = createFillInBlankQuestion(qr);
            questionSet.add(fillInBlankQuestion);

            List<QuestionData> translateQuestion2 = createTranslateQuestion2(qr);
            questionSet.add(translateQuestion2);

            List<VocabularyWord> vocabularyWords = getVocabularyWords(qr);

            super.newQuestions.add(new QuestionSetData(questionSet, qr.companyID, qr.companyJP, vocabularyWords));
        }

    }

    private List<VocabularyWord> getVocabularyWords(QueryResult qr){
        VocabularyWord make = new VocabularyWord("", "make", "つくる",
                formatSentenceEN(qr), formatSentenceJP(qr), KEY);
        VocabularyWord product = new VocabularyWord("", qr.productEN, qr.productJP,
                formatSentenceEN(qr), formatSentenceJP(qr), KEY);

        List<VocabularyWord> words = new ArrayList<>(2);
        words.add(make);
        words.add(product);
        return words;
    }

    private String formatSentenceJP(QueryResult qr){
        //作る/造る distinction impossible to determine?
        return qr.companyJP + "は" + qr.productJP + "をつくります。";
    }

    private String formatSentenceEN(QueryResult qr){
        String sentence = qr.companyEN + " makes " + qr.productEN + ".";
        return GrammarRules.uppercaseFirstLetterOfSentence(sentence);
    }

    //puzzle pieces for sentence puzzle question
    private List<String> puzzlePieces(QueryResult qr){
        List<String> pieces = new ArrayList<>();
        pieces.add(qr.companyEN);
        pieces.add("makes");
        pieces.add(qr.productEN);
        return pieces;
    }

    private String puzzlePiecesAnswer(QueryResult qr){
        return QuestionSerializer.serializeSentencePuzzleAnswer(puzzlePieces(qr));
    }

    private List<QuestionData> createSentencePuzzleQuestion(QueryResult qr){
        String question = this.formatSentenceJP(qr);
        List<String> choices = this.puzzlePieces(qr);
        String answer = puzzlePiecesAnswer(qr);
        QuestionData data = new QuestionData();
        data.setId("");
        data.setLessonId(lessonKey);
        data.setQuestionType(QuestionTypeMappings.SENTENCEPUZZLE);
        data.setQuestion(question);
        data.setChoices(choices);
        data.setAnswer(answer);
        data.setAcceptableAnswers(null);

        List<QuestionData> dataList = new ArrayList<>();
        dataList.add(data);
        return dataList;
    }

    private List<QuestionData> createSpellingQuestion(QueryResult qr){
        String question = qr.productJP;
        String answer = qr.productEN;
        QuestionData data = new QuestionData();
        data.setId("");
        data.setLessonId(lessonKey);

        data.setQuestionType(QuestionTypeMappings.SPELLING);
        data.setQuestion(question);
        data.setChoices(null);
        data.setAnswer(answer);
        data.setAcceptableAnswers(null);

        List<QuestionData> dataList = new ArrayList<>();
        dataList.add(data);
        return dataList;
    }

    private List<QuestionData> createTranslateQuestion(QueryResult qr){
        String question = qr.productJP;
        String answer = qr.productEN;
        QuestionData data = new QuestionData();
        data.setId("");
        data.setLessonId(lessonKey);

        data.setQuestionType(QuestionTypeMappings.TRANSLATEWORD);
        data.setQuestion(question);
        data.setChoices(null);
        data.setAnswer(answer);
        data.setAcceptableAnswers(null);

        List<QuestionData> dataList = new ArrayList<>();
        dataList.add(data);

        return dataList;
    }

    private String fillInBlankQuestion(QueryResult qr){
        String sentence = qr.companyEN + " " + QuestionUniqueMarkers.FILL_IN_BLANK_INPUT_TEXT +
                " " + qr.productEN + ".";
        return GrammarRules.uppercaseFirstLetterOfSentence(sentence);
    }

    private String fillInBlankAnswer(){
        return "makes";
    }

    private List<String> fillInBlankAlternateAnswers(){
        List<String> answers = new ArrayList<>(1);
        answers.add("make");
        return answers;
    }

    private List<QuestionData> createFillInBlankQuestion(QueryResult qr){
        String question = fillInBlankQuestion(qr);
        String answer = fillInBlankAnswer();
        List<String> acceptableAnswers = fillInBlankAlternateAnswers();
        QuestionData data = new QuestionData();
        data.setId("");
        data.setLessonId(lessonKey);

        data.setQuestionType(QuestionTypeMappings.FILLINBLANK_INPUT);
        data.setQuestion(question);
        data.setChoices(null);
        data.setAnswer(answer);
        data.setAcceptableAnswers(acceptableAnswers);

        List<QuestionData> dataList = new ArrayList<>();
        dataList.add(data);

        return dataList;
    }


    private String translateQuestion2(QueryResult qr){
        return formatSentenceJP(qr);
    }

    private String translateAnswer2(QueryResult qr){
        return formatSentenceEN(qr);
    }

    //accept 'make' instead of 'makes'
    private List<String> translateAcceptableAnswers2(QueryResult qr){
        String acceptableAnswer = qr.productEN + " make " + qr.companyEN + ".";
        acceptableAnswer = GrammarRules.uppercaseFirstLetterOfSentence(acceptableAnswer);
        List<String> acceptableAnswers = new ArrayList<>(1);
        acceptableAnswers.add(acceptableAnswer);
        return acceptableAnswers;
    }

    private List<QuestionData> createTranslateQuestion2(QueryResult qr){
        String question = translateQuestion2(qr);
        String answer = translateAnswer2(qr);
        List<String> acceptableAnswers = translateAcceptableAnswers2(qr);
        QuestionData data = new QuestionData();
        data.setId("");
        data.setLessonId(lessonKey);

        data.setQuestionType(QuestionTypeMappings.TRANSLATEWORD);
        data.setQuestion(question);
        data.setChoices(null);
        data.setAnswer(answer);
        data.setAcceptableAnswers(acceptableAnswers);

        List<QuestionData> dataList = new ArrayList<>();
        dataList.add(data);

        return dataList;
    }

    private List<String> translateAcceptableAnswersGeneric(){
        List<String> acceptableAnswers = new ArrayList<>(5);
        acceptableAnswers.add("作る");
        acceptableAnswers.add("造る");
        acceptableAnswers.add("造ります");
        acceptableAnswers.add("作ります");
        acceptableAnswers.add("つくります");
        return acceptableAnswers;
    }

    private List<QuestionData> createTranslateQuestionGeneric(){
        String question = "make";
        String answer = "つくる";
        List<String> acceptableAnswers = translateAcceptableAnswersGeneric();
        QuestionData data = new QuestionData();
        data.setId("");
        data.setLessonId(lessonKey);
        data.setQuestionType(QuestionTypeMappings.TRANSLATEWORD);
        data.setQuestion(question);
        data.setChoices(null);
        data.setAnswer(answer);
        data.setAcceptableAnswers(acceptableAnswers);

        List<QuestionData> dataList = new ArrayList<>();
        dataList.add(data);

        return dataList;
    }

    @Override
    protected List<List<QuestionData>> getPreGenericQuestions(){
        List<QuestionData> translateQuestion = createTranslateQuestionGeneric();

        List<List<QuestionData>> questionSet = new ArrayList<>(1);
        questionSet.add(translateQuestion);
        return questionSet;
    }
}