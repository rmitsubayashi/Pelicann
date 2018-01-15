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


public class PLACE_is_a_country_city extends Lesson {
    public static final String KEY = "PLACE_is_a_country_city";
    private final List<QueryResult> queryResults = new ArrayList<>();
    private class QueryResult {

        private final String placeID;
        private final String placeEN;
        private final String placeJP;
        private final String countryCityEN;
        private final String countryCityJP;
        private final boolean isCountry;

        private QueryResult(
                String placeID,
                String placeEN,
                String placeJP,
                boolean isCountry
        ) {

            this.placeID = placeID;
            this.placeEN = placeEN;
            this.placeJP = placeJP;
            this.countryCityEN = isCountry ? "country" : "city";
            this.countryCityJP = isCountry ? "国" : "都市";
            this.isCountry = isCountry;
        }
    }



    public PLACE_is_a_country_city(EndpointConnectorReturnsXML connector, Database db, LessonListener listener){

        super(connector, db, listener);
        super.categoryOfQuestion = WikiDataEntity.CLASSIFICATION_PLACE;
        super.questionSetsToPopulate = 2;
        super.lessonKey = KEY;
        super.questionOrder = LessonInstanceData.QUESTION_ORDER_ORDER_BY_QUESTION;

    }

    @Override
    protected String getSPARQLQuery(){
        return "SELECT DISTINCT ?place ?placeEN ?placeLabel ?instance " +
                " WHERE " +
                "{" +
                "    {?place wdt:P31 wd:Q6256 . " +
                "    BIND ('country' as ?instance)} UNION " + //is a country
                "    {?place wdt:P31/wdt:P279* wd:Q515 . " +
                "    BIND ('city' as ?instance)} . " + //or city
                "    ?place rdfs:label ?placeEN . " +
                "    FILTER (LANG(?placeEN) = '" +
                WikiBaseEndpointConnector.ENGLISH + "') . " +
                "    SERVICE wikibase:label { bd:serviceParam wikibase:language '" +
                WikiBaseEndpointConnector.LANGUAGE_PLACEHOLDER + "', " + //JP label if possible
                "    '" + WikiBaseEndpointConnector.ENGLISH + "'} . " + //fallback language is English

                "    BIND (wd:%s as ?place) . " + //binding the ID of entity as place

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
            String placeID = SPARQLDocumentParserHelper.findValueByNodeName(head, "place");

            placeID = WikiDataEntity.getWikiDataIDFromReturnedResult(placeID);
            String placeEN = SPARQLDocumentParserHelper.findValueByNodeName(head, "placeEN");
            String placeJP = SPARQLDocumentParserHelper.findValueByNodeName(head, "placeLabel");
            String cityOrCountryString = SPARQLDocumentParserHelper.findValueByNodeName(head, "instance");
            boolean isCountry = cityOrCountryString.equals("country");
            QueryResult qr = new QueryResult(placeID, placeEN, placeJP, isCountry);

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

            List<QuestionData> fillInBlankMultipleChoice = createFillInBlankMultipleChoiceQuestion(qr);
            questionSet.add(fillInBlankMultipleChoice);

            List<QuestionData> fillInBlankInput = createFillInBlankInputQuestion(qr);
            questionSet.add(fillInBlankInput);

            List<QuestionData> trueFalse = createTrueFalseQuestion(qr);
            questionSet.add(trueFalse);

            List<VocabularyWord> vocabularyWords = getVocabularyWords(qr);

            super.newQuestions.add(new QuestionSetData(questionSet, qr.placeID, qr.placeJP, vocabularyWords));
        }

    }

    private List<VocabularyWord> getVocabularyWords(QueryResult qr){
        VocabularyWord countryCity = new VocabularyWord("",qr.countryCityEN, qr.countryCityJP,
                formatSentenceEN(qr), formatSentenceJP(qr), KEY);
        VocabularyWord place = new VocabularyWord("", qr.placeEN, qr.placeJP,
                formatSentenceEN(qr), formatSentenceJP(qr), KEY);

        List<VocabularyWord> words = new ArrayList<>(2);
        words.add(place);
        words.add(countryCity);
        return words;
    }

    private String formatSentenceEN(QueryResult qr){
        return qr.placeEN + " is a " + qr.countryCityEN + ".";
    }

    private String formatSentenceJP(QueryResult qr){
        return qr.placeJP + "は" + qr.countryCityJP + "です。";
    }

    private String fillInBlankMultipleChoiceQuestion(QueryResult qr){
        String place = qr.placeEN;
        if (qr.isCountry){
            place = GrammarRules.definiteArticleBeforeCountry(place);
        }
        String sentence = place + " is a " + QuestionUniqueMarkers.FILL_IN_BLANK_MULTIPLE_CHOICE + ".";
        return GrammarRules.uppercaseFirstLetterOfSentence(sentence);
    }

    private List<String> fillInBlankMultipleChoiceChoices(){
        List<String> choices = new ArrayList<>(2);
        choices.add("country");
        choices.add("city");
        return choices;
    }

    private List<QuestionData> createFillInBlankMultipleChoiceQuestion(QueryResult qr){
        String question = this.fillInBlankMultipleChoiceQuestion(qr);
        List<String> choices = fillInBlankMultipleChoiceChoices();
        String answer = fillInBlankAnswer(qr);
        QuestionData data = new QuestionData();
        data.setId("");
        data.setLessonId(super.lessonKey);

        data.setQuestionType(QuestionTypeMappings.FILLINBLANK_MULTIPLECHOICE);
        data.setQuestion(question);
        data.setChoices(choices);
        data.setAnswer(answer);
        data.setAcceptableAnswers(null);

        data.setFeedback(null);
        List<QuestionData> questionDataList = new ArrayList<>();
        questionDataList.add(data);
        return questionDataList;

    }

    private String fillInBlankInputQuestion(QueryResult qr){
        String sentence1 = formatSentenceJP(qr);
        String place = qr.placeEN;
        if (qr.isCountry){
            place = GrammarRules.definiteArticleBeforeCountry(place);
        }
        String sentence2 = place + " is a " + QuestionUniqueMarkers.FILL_IN_BLANK_INPUT_TEXT + ".";
        return sentence1 + "\n\n" +
                GrammarRules.uppercaseFirstLetterOfSentence(sentence2);
    }

    private String fillInBlankAnswer(QueryResult qr){
        return qr.countryCityEN;
    }

    private List<QuestionData> createFillInBlankInputQuestion(QueryResult qr){
        String question = this.fillInBlankInputQuestion(qr);

        String answer = fillInBlankAnswer(qr);
        QuestionData data = new QuestionData();
        data.setId("");
        data.setLessonId(super.lessonKey);

        data.setQuestionType(QuestionTypeMappings.FILLINBLANK_INPUT);
        data.setQuestion(question);
        data.setChoices(null);
        data.setAnswer(answer);
        data.setAcceptableAnswers(null);

        data.setFeedback(null);
        List<QuestionData> questionDataList = new ArrayList<>();
        questionDataList.add(data);
        return questionDataList;

    }

    private String trueFalseQuestion(QueryResult qr, boolean isTrue){
        String countryOrCity;
        if (isTrue){
            countryOrCity = qr.countryCityEN;
        } else {
            if (qr.isCountry){
                countryOrCity = "city";
            }else {
                countryOrCity = "country";
            }
        }
        String place = qr.placeEN;
        if (qr.isCountry){
            place = GrammarRules.definiteArticleBeforeCountry(place);
        }
        String sentence = place + " is a " + countryOrCity + ".";
        return GrammarRules.uppercaseFirstLetterOfSentence(sentence);
    }

    private List<QuestionData> createTrueFalseQuestion(QueryResult qr){
        //one true and one false question
        List<QuestionData> questionDataList = new ArrayList<>();
        String question = this.trueFalseQuestion(qr, true);
        String answer = QuestionSerializer.serializeTrueFalseAnswer(true);
        QuestionData data = new QuestionData();
        data.setId("");
        data.setLessonId(super.lessonKey);

        data.setQuestionType(QuestionTypeMappings.TRUEFALSE);
        data.setQuestion(question);
        data.setChoices(null);
        data.setAnswer(answer);
        data.setAcceptableAnswers(null);

        data.setFeedback(null);
        questionDataList.add(data);

        question = this.trueFalseQuestion(qr, false);
        answer = QuestionSerializer.serializeTrueFalseAnswer(false);
        data = new QuestionData();
        data.setId("");
        data.setLessonId(super.lessonKey);

        data.setQuestionType(QuestionTypeMappings.TRUEFALSE);
        data.setQuestion(question);
        data.setChoices(null);
        data.setAnswer(answer);
        data.setAcceptableAnswers(null);

        data.setFeedback(null);
        questionDataList.add(data);

        return questionDataList;

    }

    private List<String> translateAcceptableAnswersGeneric(){
        List<String> acceptableAnswers = new ArrayList<>(1);
        acceptableAnswers.add("国家");
        return acceptableAnswers;
    }

    private List<QuestionData> createTranslateQuestionGeneric(){
        String question = "country";
        String answer = "国";
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

    private List<String> translateAcceptableAnswersGeneric2(){
        List<String> acceptableAnswers = new ArrayList<>(1);
        acceptableAnswers.add("市");
        return acceptableAnswers;
    }

    private List<QuestionData> createTranslateQuestionGeneric2(){
        String question = "city";
        String answer = "都市";
        List<String> acceptableAnswers = translateAcceptableAnswersGeneric2();
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
        List<List<QuestionData>> questionSet = new ArrayList<>(2);
        List<QuestionData> translateQuestion = createTranslateQuestionGeneric();
        questionSet.add(translateQuestion);
        List<QuestionData> translateQuestion2 = createTranslateQuestionGeneric2();
        questionSet.add(translateQuestion2);

        return questionSet;
    }

}