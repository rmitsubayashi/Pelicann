package pelicann.linnca.com.corefunctionality.lessongeneration.lessons;

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

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class COUNTRY_drives_on_the_left_right extends Lesson{
    public static final String KEY = "COUNTRY_drives_on_the_left_right";

    private final List<QueryResult> queryResults = new ArrayList<>();
    private class QueryResult {
        private final String countryID;
        private final String countryEN;
        private final String countryJP;
        private final String sideEN;
        private final String sideJP;

        private QueryResult(
                String countryID,
                String countryEN,
                String countryJP,
                String sideEN,
                String sideJP)
        {
            this.countryID = countryID;
            this.countryEN = countryEN;
            this.countryJP = countryJP;
            this.sideEN = sideEN;
            this.sideJP = sideJP;
        }
    }

    public COUNTRY_drives_on_the_left_right(EndpointConnectorReturnsXML connector, Database db, LessonListener listener){
        super(connector, db, listener);
        super.questionSetsToPopulate = 3;
        super.categoryOfQuestion = WikiDataEntity.CLASSIFICATION_PLACE;
        super.lessonKey = KEY;
        super.questionOrder = LessonInstanceData.QUESTION_ORDER_ORDER_BY_SET;
    }

    @Override
    protected String getSPARQLQuery(){
        //find country name and blood type
        return "SELECT ?country ?countryLabel ?countryEN " +
                " ?sideEN ?sideLabel " +
                "WHERE " +
                "{" +
                "    ?country wdt:P1622 ?side . " + //has a driving side
                "    ?country rdfs:label ?countryEN . " +
                "    ?side rdfs:label ?sideEN . " +
                "    FILTER (LANG(?countryEN) = '" +
                WikiBaseEndpointConnector.ENGLISH + "') . " +
                "    FILTER (LANG(?sideEN) = '" +
                WikiBaseEndpointConnector.ENGLISH + "') . " +
                "    SERVICE wikibase:label { bd:serviceParam wikibase:language '" +
                WikiBaseEndpointConnector.LANGUAGE_PLACEHOLDER + "', '" + //JP label if possible
                WikiBaseEndpointConnector.ENGLISH + "'} . " + //fallback language is English
                "    BIND (wd:%s as ?country) . " + //binding the ID of entity as ?country
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
            String countryID = SPARQLDocumentParserHelper.findValueByNodeName(head, "country");
            countryID = WikiDataEntity.getWikiDataIDFromReturnedResult(countryID);
            String countryEN = SPARQLDocumentParserHelper.findValueByNodeName(head, "countryEN");
            String countryJP = SPARQLDocumentParserHelper.findValueByNodeName(head, "countryLabel");
            String sideEN = SPARQLDocumentParserHelper.findValueByNodeName(head, "sideEN");
            String sideJP = SPARQLDocumentParserHelper.findValueByNodeName(head, "sideLabel");

            QueryResult qr = new QueryResult(countryID, countryEN, countryJP, sideEN, sideJP);
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

            List<QuestionData> trueFalseQuestion = createTrueFalseQuestion(qr);
            questionSet.add(trueFalseQuestion);

            List<QuestionData> fillInBlankQuestion = createFillInBlankQuestion(qr);
            questionSet.add(fillInBlankQuestion);

            List<QuestionData> translateQuestion2 = createTranslateQuestion2(qr);
            questionSet.add(translateQuestion2);

            super.newQuestions.add(new QuestionSetData(questionSet, qr.countryID, qr.countryJP, null));
        }

    }

    private String formatSentenceJP(QueryResult qr){
        return qr.countryJP + "は" + qr.sideJP + "側通行です。";
    }

    private String formatSentenceEN(QueryResult qr){
        String sentence = GrammarRules.definiteArticleBeforeCountry(qr.countryEN) + " drives on the " + qr.sideEN + ".";
        return GrammarRules.uppercaseFirstLetterOfSentence(sentence);
    }

    private List<QuestionData> createTranslateQuestionGeneric(){
        String question = "drive";
        String answer = "運転する";
        List<String> acceptableAnswers = new ArrayList<>(1);
        acceptableAnswers.add("運転");
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

    private List<QuestionData> createSpellingQuestionGeneric1(){
        String question = "左";
        String answer = "left";
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

    private List<QuestionData> createSpellingQuestionGeneric2(){
        String question = "右";
        String answer = "right";
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

    @Override
    protected List<List<QuestionData>> getPreGenericQuestions(){
        List<QuestionData> translateQuestion = createTranslateQuestionGeneric();
        List<QuestionData> spellingQuestion1 = createSpellingQuestionGeneric1();
        List<QuestionData> spellingQuestion2 = createSpellingQuestionGeneric2();

        List<List<QuestionData>> questionSet = new ArrayList<>(3);
        questionSet.add(translateQuestion);
        questionSet.add(spellingQuestion1);
        questionSet.add(spellingQuestion2);

        return questionSet;
    }

    @Override
    protected void shufflePreGenericQuestions(List<List<QuestionData>> preGenericQuestions){
        List<List<QuestionData>> spelling = preGenericQuestions.subList(1,3);
        Collections.shuffle(spelling);

    }

    //puzzle pieces for sentence puzzle question
    private List<String> puzzlePieces(QueryResult qr){
        List<String> pieces = new ArrayList<>();
        pieces.add(GrammarRules.definiteArticleBeforeCountry(qr.countryEN));
        pieces.add("drives");
        pieces.add("on");
        pieces.add("the " + qr.sideEN);
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

    private String trueFalseQuestion(QueryResult qr, boolean isTrue){
        if (isTrue){
            return formatSentenceEN(qr);
        } else {
            String opposite = qr.sideEN.equals("left") ? "right" : "left";
            String wrongSentence = GrammarRules.definiteArticleBeforeCountry(qr.countryEN) +
                    " drives on the " + opposite + ".";
            wrongSentence = GrammarRules.uppercaseFirstLetterOfSentence(wrongSentence);
            return wrongSentence;
        }
    }

    private List<QuestionData> createTrueFalseQuestion(QueryResult qr){
        List<QuestionData> dataList = new ArrayList<>();

        String question = trueFalseQuestion(qr, true);
        String answer = QuestionSerializer.serializeTrueFalseAnswer(true);
        QuestionData data = new QuestionData();
        data.setId("");
        data.setLessonId(lessonKey);

        data.setQuestionType(QuestionTypeMappings.TRUEFALSE);
        data.setQuestion(question);
        data.setChoices(null);
        data.setAnswer(answer);
        data.setAcceptableAnswers(null);
        dataList.add(data);

        question = trueFalseQuestion(qr, false);
        answer = QuestionSerializer.serializeTrueFalseAnswer(false);
        data = new QuestionData();
        data.setId("");
        data.setLessonId(lessonKey);

        data.setQuestionType(QuestionTypeMappings.TRUEFALSE);
        data.setQuestion(question);
        data.setChoices(null);
        data.setAnswer(answer);
        data.setAcceptableAnswers(null);
        dataList.add(data);

        return dataList;
    }

    private String fillInBlankMultipleChoiceQuestion(QueryResult qr){
        String sentence = GrammarRules.definiteArticleBeforeCountry(qr.countryEN) +
                " drives " + QuestionUniqueMarkers.FILL_IN_BLANK_MULTIPLE_CHOICE +
                " the " + qr.sideEN + ".";
        return GrammarRules.uppercaseFirstLetterOfSentence(sentence);
    }

    private List<String> fillInBlankMultipleChoiceChoices(){
        List<String> choices = new ArrayList<>(4);
        choices.add("on");
        choices.add("from");
        choices.add("for");
        choices.add("at");
        return choices;
    }

    private String fillInBlankMultipleChoiceAnswer(){
        return "on";
    }

    private List<QuestionData> createFillInBlankQuestion(QueryResult qr){
        String question = fillInBlankMultipleChoiceQuestion(qr);
        String answer = fillInBlankMultipleChoiceAnswer();
        List<String> choices = fillInBlankMultipleChoiceChoices();
        QuestionData data = new QuestionData();
        data.setId("");
        data.setLessonId(lessonKey);

        data.setQuestionType(QuestionTypeMappings.FILLINBLANK_MULTIPLECHOICE);
        data.setQuestion(question);
        data.setChoices(choices);
        data.setAnswer(answer);
        data.setAcceptableAnswers(null);

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

    private List<String> translateAcceptableAnswers2(QueryResult qr){
        String acceptableAnswer1 = qr.countryEN + " drive on the " + qr.sideEN + ".";
        String acceptableAnswer2 = qr.countryEN + " drives on " + qr.sideEN + ".";
        String acceptableAnswer3 = qr.countryEN + " drive on " + qr.sideEN + ".";

        List<String> acceptableAnswers = new ArrayList<>(6);
        acceptableAnswers.add(acceptableAnswer1);
        acceptableAnswers.add(acceptableAnswer2);
        acceptableAnswers.add(acceptableAnswer3);

        if (!qr.countryEN.equals(GrammarRules.definiteArticleBeforeCountry(qr.countryEN))) {
            String acceptableAnswer4 = GrammarRules.definiteArticleBeforeCountry(qr.countryEN) +
                    " drive on the " + qr.sideEN + ".";
            String acceptableAnswer5 = GrammarRules.definiteArticleBeforeCountry(qr.countryEN) +
                    " drives on " + qr.sideEN + ".";
            String acceptableAnswer6 = GrammarRules.definiteArticleBeforeCountry(qr.countryEN) +
                    " drive on " + qr.sideEN + ".";
            acceptableAnswers.add(acceptableAnswer4);
            acceptableAnswers.add(acceptableAnswer5);
            acceptableAnswers.add(acceptableAnswer6);
        }

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
}