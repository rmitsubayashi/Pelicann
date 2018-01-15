package pelicann.linnca.com.corefunctionality.lessongeneration.lessons;

import pelicann.linnca.com.corefunctionality.connectors.EndpointConnectorReturnsXML;
import pelicann.linnca.com.corefunctionality.connectors.SPARQLDocumentParserHelper;
import pelicann.linnca.com.corefunctionality.connectors.WikiBaseEndpointConnector;
import pelicann.linnca.com.corefunctionality.connectors.WikiDataSPARQLConnector;
import pelicann.linnca.com.corefunctionality.db.Database;
import pelicann.linnca.com.corefunctionality.lessondetails.LessonInstanceData;
import pelicann.linnca.com.corefunctionality.lessongeneration.Lesson;
import pelicann.linnca.com.corefunctionality.questions.ChatQuestionItem;
import pelicann.linnca.com.corefunctionality.questions.QuestionData;
import pelicann.linnca.com.corefunctionality.questions.QuestionResponseChecker;
import pelicann.linnca.com.corefunctionality.questions.QuestionSerializer;
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

/*
* Goals for this lesson:
* The user should be able to greet each other
* and introduce themselves
* */

public class Hello_my_name_is_NAME extends Lesson {
    public static final String KEY = "Hello_my_name_is_NAME";

    private final List<QueryResult> queryResults = new ArrayList<>();
    private class QueryResult {
        private final String personID;
        private final String personEN;
        private final String personJP;

        private QueryResult(
                String personID,
                String personEN,
                String personJP)
        {
            this.personID = personID;
            this.personEN = personEN;
            this.personJP = personJP;
        }
    }

    public Hello_my_name_is_NAME(EndpointConnectorReturnsXML connector, Database db, LessonListener listener){
        super(connector, db, listener);
        super.questionSetsToPopulate = 2;
        super.categoryOfQuestion = WikiDataEntity.CLASSIFICATION_PERSON;
        super.lessonKey = KEY;
        super.questionOrder = LessonInstanceData.QUESTION_ORDER_ORDER_BY_SET;

    }

    @Override
    protected String getSPARQLQuery(){
        //find person name and blood type
        return "SELECT ?person ?personLabel ?personEN " +
                "WHERE " +
                "{" +
                "    ?person rdfs:label ?personEN . " +
                "    FILTER (LANG(?personEN) = '" +
                WikiBaseEndpointConnector.ENGLISH + "') . " +
                "    SERVICE wikibase:label { bd:serviceParam wikibase:language '" +
                WikiBaseEndpointConnector.LANGUAGE_PLACEHOLDER + "', '" + //JP label if possible
                WikiBaseEndpointConnector.ENGLISH + "'} . " + //fallback language is English
                "    BIND (wd:%s as ?person) . " + //binding the ID of entity as ?person
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
            String personID = SPARQLDocumentParserHelper.findValueByNodeName(head, "person");
            personID = WikiDataEntity.getWikiDataIDFromReturnedResult(personID);
            String personEN = SPARQLDocumentParserHelper.findValueByNodeName(head, "personEN");
            String personJP = SPARQLDocumentParserHelper.findValueByNodeName(head, "personLabel");

            QueryResult qr = new QueryResult(personID, personEN, personJP);
            queryResults.add(qr);
        }
    }

    @Override
    protected synchronized int getQueryResultCt(){ return queryResults.size(); }

    @Override
    protected synchronized void createQuestionsFromResults(){
        for (QueryResult qr : queryResults){
            List<List<QuestionData>> questionSet = new ArrayList<>();
            List<QuestionData> chatMultipleChoice = createChatMultipleChoiceQuestion(qr);
            questionSet.add(chatMultipleChoice);
            List<QuestionData> chat = createChatQuestion(qr);
            questionSet.add(chat);
            List<QuestionData> sentencePuzzle = createSentencePuzzleQuestion(qr);
            questionSet.add(sentencePuzzle);
            List<QuestionData> fillInBlank = createFillInBlankQuestion(qr);
            questionSet.add(fillInBlank);
            List<VocabularyWord> vocabularyWords = getVocabularyWords(qr);
            super.newQuestions.add(new QuestionSetData(questionSet, qr.personID, qr.personJP, vocabularyWords));
        }

    }

    private String formatSentenceEN(QueryResult qr){
        return "Hello. My name is " + qr.personEN + ".";
    }

    private String formatSentenceJP(QueryResult qr){
        return "こんにちは。私の名前は" + qr.personJP + "です。";
    }

    private List<VocabularyWord> getVocabularyWords(QueryResult qr){
        VocabularyWord hello = new VocabularyWord("","hello", "こんにちは",
                formatSentenceEN(qr), formatSentenceJP(qr), KEY);
        VocabularyWord my = new VocabularyWord("","my", "私の",
                "My name is " + qr.personEN + ".", "私の名前は" + qr.personJP + "です。", KEY);
        VocabularyWord name = new VocabularyWord("", "name","名前",
                "My name is " + qr.personEN + ".", "私の名前は" + qr.personJP + "です。", KEY);
        VocabularyWord is = new VocabularyWord("", "is","~は",
                "My name is " + qr.personEN + ".", "私の名前は" + qr.personJP + "です。", KEY);
        List<VocabularyWord> words = new ArrayList<>(4);
        words.add(hello);
        words.add(my);
        words.add(name);
        words.add(is);
        return words;
    }

    //straight-forward question so the user understands that this is a natural response
    private List<QuestionData> createChatMultipleChoiceQuestion(QueryResult qr){
        String from = qr.personJP;
        ChatQuestionItem chatItem1 = new ChatQuestionItem(false, "hello");
        ChatQuestionItem chatItem2 = new ChatQuestionItem(true, ChatQuestionItem.USER_INPUT);
        List<ChatQuestionItem> chatItems = new ArrayList<>(2);
        chatItems.add(chatItem1);
        chatItems.add(chatItem2);
        String question = QuestionSerializer.serializeChatQuestion(from, chatItems);
        String answer = "hello";
        List<String> choices = new ArrayList<>(1);
        choices.add(answer);
        QuestionData data = new QuestionData();
        data.setId("");
        data.setLessonId(lessonKey);
        data.setQuestionType(QuestionTypeMappings.CHAT_MULTIPLECHOICE);
        data.setQuestion(question);
        data.setChoices(choices);
        data.setAnswer(answer);
        data.setAcceptableAnswers(null);

        List<QuestionData> dataList = new ArrayList<>();
        dataList.add(data);
        return dataList;
    }

    //enforces the idea of the previous chat question and also practices spelling
    private List<QuestionData> createChatQuestion(QueryResult qr){
        String from = qr.personJP;
        ChatQuestionItem chatItem1 = new ChatQuestionItem(false, "hello");
        ChatQuestionItem chatItem2 = new ChatQuestionItem(true, ChatQuestionItem.USER_INPUT);
        List<ChatQuestionItem> chatItems = new ArrayList<>(2);
        chatItems.add(chatItem1);
        chatItems.add(chatItem2);
        String question = QuestionSerializer.serializeChatQuestion(from, chatItems);
        String answer = "hello";
        QuestionData data = new QuestionData();
        data.setId("");
        data.setLessonId(lessonKey);

        data.setQuestionType(QuestionTypeMappings.CHAT);
        data.setQuestion(question);
        data.setChoices(null);
        data.setAnswer(answer);
        data.setAcceptableAnswers(null);

        List<QuestionData> dataList = new ArrayList<>();
        dataList.add(data);
        return dataList;
    }

    //this introduces the whole phrase
    private List<String> puzzlePieces(QueryResult qr){
        List<String> pieces = new ArrayList<>();
        pieces.add("hello");
        pieces.add("my name is");
        pieces.add(qr.personEN);
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

    //lets users practice  the latter part of introductions
    private String fillInBlankQuestion(QueryResult qr){
        return "Hello. " +
                QuestionUniqueMarkers.FILL_IN_BLANK_INPUT_TEXT + " " + qr.personEN + ".";
    }

    private String fillInBlankAnswer(){
        return "My name is";
    }

    private List<QuestionData> createFillInBlankQuestion(QueryResult qr){
        String question = this.fillInBlankQuestion(qr);
        String answer = fillInBlankAnswer();
        QuestionData data = new QuestionData();
        data.setId("");
        data.setLessonId(lessonKey);

        data.setQuestionType(QuestionTypeMappings.FILLINBLANK_INPUT);
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
        List<QuestionData> spellingSuggestiveQuestion = createSpellingSuggestiveQuestion();
        List<QuestionData> spellingQuestion = createSpellingQuestion();

        List<List<QuestionData>> questionSet = new ArrayList<>(2);
        questionSet.add(spellingSuggestiveQuestion);
        questionSet.add(spellingQuestion);
        return questionSet;

    }

    private List<QuestionData> createSpellingSuggestiveQuestion(){
        String question = "こんにちは";
        String answer = "hello";
        QuestionData data = new QuestionData();
        data.setId("");
        data.setLessonId(lessonKey);

        data.setQuestionType(QuestionTypeMappings.SPELLING_SUGGESTIVE);
        data.setQuestion(question);
        data.setChoices(null);
        data.setAnswer(answer);
        data.setAcceptableAnswers(null);

        List<QuestionData> dataList = new ArrayList<>();
        dataList.add(data);

        return dataList;
    }

    private List<QuestionData> createSpellingQuestion(){
        String question = "こんにちは";
        String answer = "hello";
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
    protected List<List<QuestionData>> getPostGenericQuestions(){
        List<QuestionData> instructionsQuestion = createInstructionQuestion();
        List<List<QuestionData>> questionSet = new ArrayList<>(1);
        questionSet.add(instructionsQuestion);
        return questionSet;
    }

    //lets the user freely introduce themselves
    private String instructionQuestionQuestion(){
        return "自己紹介をしてください";
    }

    private String instructionQuestionAnswer(){
        return "Hello. My name is " + QuestionResponseChecker.ANYTHING + ".";
    }

    private List<String> instructionQuestionAcceptableAnswers(){
        String acceptableAnswer1 = "Hello my name is " + QuestionResponseChecker.ANYTHING + ".";
        String acceptableAnswer2 = "Hello, my name is " + QuestionResponseChecker.ANYTHING + ".";
        String acceptableAnswer3 = "My name is " + QuestionResponseChecker.ANYTHING + ".";
        List<String> acceptableAnswers = new ArrayList<>(3);
        acceptableAnswers.add(acceptableAnswer1);
        acceptableAnswers.add(acceptableAnswer2);
        acceptableAnswers.add(acceptableAnswer3);
        return acceptableAnswers;

    }

    private List<QuestionData> createInstructionQuestion(){
        String question = this.instructionQuestionQuestion();
        String answer = instructionQuestionAnswer();
        List<String> acceptableAnswers = instructionQuestionAcceptableAnswers();
        QuestionData data = new QuestionData();
        data.setId("");
        data.setLessonId(lessonKey);

        data.setQuestionType(QuestionTypeMappings.INSTRUCTIONS);
        data.setQuestion(question);
        data.setChoices(null);
        data.setAnswer(answer);
        data.setAcceptableAnswers(acceptableAnswers);

        List<QuestionData> dataList = new ArrayList<>();
        dataList.add(data);

        return dataList;
    }
}