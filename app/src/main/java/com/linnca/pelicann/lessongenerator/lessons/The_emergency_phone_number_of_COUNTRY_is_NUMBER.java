package com.linnca.pelicann.lessongenerator.lessons;


import com.linnca.pelicann.connectors.EndpointConnectorReturnsXML;
import com.linnca.pelicann.connectors.SPARQLDocumentParserHelper;
import com.linnca.pelicann.connectors.WikiBaseEndpointConnector;
import com.linnca.pelicann.connectors.WikiDataSPARQLConnector;
import com.linnca.pelicann.db.Database;
import com.linnca.pelicann.lessongenerator.FeedbackPair;
import com.linnca.pelicann.lessongenerator.GrammarRules;
import com.linnca.pelicann.lessongenerator.Lesson;
import com.linnca.pelicann.lessongenerator.StringUtils;
import com.linnca.pelicann.questions.QuestionData;
import com.linnca.pelicann.questions.QuestionSetData;
import com.linnca.pelicann.questions.Question_FillInBlank_Input;
import com.linnca.pelicann.questions.Question_Spelling_Suggestive;
import com.linnca.pelicann.questions.Question_TranslateWord;
import com.linnca.pelicann.userinterests.WikiDataEntity;
import com.linnca.pelicann.vocabulary.VocabularyWord;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;



public class The_emergency_phone_number_of_COUNTRY_is_NUMBER extends Lesson {
    public static final String KEY = "The_emergency_phone_number_of_COUNTRY_is_NUMBER";
    private final List<QueryResult> queryResults = new ArrayList<>();
    private class QueryResult {

        private final String countryID;
        private final String countryEN;
        private final String countryJP;
        private final String phoneNumber;

        private QueryResult(
                String countryID,
                String countryEN,
                String countryJP,
                String phoneNumber
        ) {

            this.countryID = countryID;
            this.countryEN = countryEN;
            this.countryJP = countryJP;
            this.phoneNumber = phoneNumber;
        }
    }



    public The_emergency_phone_number_of_COUNTRY_is_NUMBER(EndpointConnectorReturnsXML connector, Database db, LessonListener listener){

        super(connector, db, listener);
        super.categoryOfQuestion = WikiDataEntity.CLASSIFICATION_PLACE;
        super.questionSetsToPopulate = 2;
        super.lessonKey = KEY;

    }

    @Override
    protected String getSPARQLQuery(){
        return "SELECT ?country ?countryEN ?countryLabel " +
                " ?phoneNumberLabel " +
                "WHERE " +
                "{" +
                "    ?country wdt:P31 wd:Q6256 . " + //is a country
                "    ?country wdt:P2852 ?phoneNumber . " + //has a emergency phone number
                "    ?country rdfs:label ?countryEN . " +
                "    FILTER (LANG(?countryEN) = '" +
                    WikiBaseEndpointConnector.ENGLISH + "') . " +
                "    SERVICE wikibase:label { bd:serviceParam wikibase:language '" +
                WikiBaseEndpointConnector.LANGUAGE_PLACEHOLDER + "', " + //foreign label if possible
                "    '" + WikiBaseEndpointConnector.ENGLISH + "'} . " + //fallback language is English

                "    BIND (wd:%s as ?country) . " + //binding the ID of entity as ?city

                "} ";



    }



    @Override

    protected synchronized void processResultsIntoClassWrappers(Document document) {

        NodeList allResults = document.getElementsByTagName(
                WikiDataSPARQLConnector.RESULT_TAG
        );

        int resultLength = allResults.getLength();

        //there may be more than one emergency phone number for a country
        //ie 110 & 119
        for (int i=0; i<resultLength; i++) {
            Node head = allResults.item(i);
            String countryID = SPARQLDocumentParserHelper.findValueByNodeName(head, "country");

            countryID = WikiDataEntity.getWikiDataIDFromReturnedResult(countryID);
            String countryEN = SPARQLDocumentParserHelper.findValueByNodeName(head, "countryEN");
            String countryJP = SPARQLDocumentParserHelper.findValueByNodeName(head, "countryLabel");
            String phoneNumber = SPARQLDocumentParserHelper.findValueByNodeName(head, "phoneNumberLabel");
            //since we may return something like 100番
            //remove all non-digits.
            //we don't need to worry about hyphens because these are all emergency numbers
            phoneNumber = phoneNumber.replaceAll("[^\\d]", "");
            QueryResult qr = new QueryResult(countryID, countryEN, countryJP, phoneNumber);

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
            List<QuestionData> spellingSuggestiveQuestion = spellingSuggestiveQuestion(qr);
            questionSet.add(spellingSuggestiveQuestion);

            List<QuestionData> translateWordQuestion = createTranslateWordQuestion(qr);
            questionSet.add(translateWordQuestion);

            List<QuestionData> fillInBlankInput1Question = createFillInBlankInputQuestion1(qr);
            questionSet.add(fillInBlankInput1Question);

            List<QuestionData> fillInBlankInput2Question = createFillInBlankInputQuestion2(qr);
            questionSet.add(fillInBlankInput2Question);

            super.newQuestions.add(new QuestionSetData(questionSet, qr.countryID, qr.countryJP, new ArrayList<VocabularyWord>()));
        }



    }

    private List<QuestionData> spellingSuggestiveQuestion(QueryResult qr){
        String question = qr.countryJP;

        String answer = qr.countryEN;
        QuestionData data = new QuestionData();
        data.setId("");
        data.setLessonId(super.lessonKey);
        
        data.setQuestionType(Question_Spelling_Suggestive.QUESTION_TYPE);
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
        String lowercaseCountry = qr.countryEN.toLowerCase();
        List<String> responses = new ArrayList<>();
        responses.add(lowercaseCountry);
        String feedback = "国の名前は大文字で始まります。\n" + lowercaseCountry + " → " + qr.countryEN;
        return new FeedbackPair(responses, feedback, FeedbackPair.EXPLICIT);
    }

    private List<QuestionData> createTranslateWordQuestion(QueryResult qr){
        String question = qr.countryJP;
        String answer = qr.countryEN;
        FeedbackPair feedbackPair = fillInBlankInputFeedback(qr);
        List<FeedbackPair> feedbackPairs = new ArrayList<>();
        feedbackPairs.add(feedbackPair);
        QuestionData data = new QuestionData();
        data.setId("");
        data.setLessonId(super.lessonKey);
        
        data.setQuestionType(Question_TranslateWord.QUESTION_TYPE);
        data.setQuestion(question);
        data.setChoices(null);
        data.setAnswer(answer);
        data.setAcceptableAnswers(null);

        data.setFeedback(feedbackPairs);
        List<QuestionData> questionDataList = new ArrayList<>();
        questionDataList.add(data);

        return questionDataList;
    }

    private String fillInBlankInput1Question(QueryResult qr){
        String phoneNumber = qr.phoneNumber;
        String supportingSentence = "英単語を記入してください";
        String sentence1 = qr.countryJP + "の緊急通報用電話番号は" + phoneNumber + "です。";
        String sentence2 = "The emergency phone number of " + qr.countryEN + " is " + Question_FillInBlank_Input.FILL_IN_BLANK_TEXT + ".";
        return supportingSentence + "\n\n" + sentence1 + "\n\n" + sentence2;
    }

    private String fillInBlankInput1Answer(QueryResult qr){
        String phoneNumber = qr.phoneNumber;
        List<String> phoneNumberWords = StringUtils.convertPhoneNumberToPhoneNumberWords(phoneNumber);
        return phoneNumberWords.get(0);
    }

    private List<String> fillInBlankInput1AcceptableAnswers(QueryResult qr){
        String phoneNumber = qr.phoneNumber;
        List<String> phoneNumberWords = StringUtils.convertPhoneNumberToPhoneNumberWords(phoneNumber);
        //remove the actual answer
        phoneNumberWords.remove(0);
        return phoneNumberWords;
    }

    private FeedbackPair fillInBlankInput1Feedback(QueryResult qr){
        List<String> responses = new ArrayList<>();
        String phoneNumberNumber = qr.phoneNumber;
        //if the user types nine hundred eleven for 911
        String numberWords = StringUtils.convertIntToWord(phoneNumberNumber);
        responses.add(numberWords);
        List<String> phoneNumberWords = StringUtils.convertPhoneNumberToPhoneNumberWords(phoneNumberNumber);
        String feedback = "電話番号は一桁ずつ分けて言います。つまり、 " + numberWords +
                " ではなく " + phoneNumberWords.get(0) + " が正解です。";

        return new FeedbackPair(responses, feedback, FeedbackPair.IMPLICIT);
    }

    private FeedbackPair fillInBlankInput1Feedback2(QueryResult qr){
        //even if the user makes a correct choice,
        //let him know if there are other options available
        List<String> responses = new ArrayList<>();
        String phoneNumber = qr.phoneNumber;
        List<String> phoneNumberWords = StringUtils.convertPhoneNumberToPhoneNumberWords(phoneNumber);
        if (phoneNumberWords.size() == 1){
            return null;
        }
        String feedback = "正解は複数あります。\n";
        for (String phoneNumberWord : phoneNumberWords){
            responses.add(phoneNumberWord);
            //cleaner alignment
            feedback += "  " + phoneNumberWord + "\n";
        }
        feedback += "詳しい解説は説明文に書いてあります。";


        return new FeedbackPair(responses, feedback, FeedbackPair.IMPLICIT);
    }

    private List<QuestionData> createFillInBlankInputQuestion1(QueryResult qr){
        String question = this.fillInBlankInput1Question(qr);

        String answer = fillInBlankInput1Answer(qr);
        List<String> acceptableAnswers = fillInBlankInput1AcceptableAnswers(qr);
        FeedbackPair feedbackPair = fillInBlankInput1Feedback(qr);
        FeedbackPair feedbackPair2 = fillInBlankInput1Feedback2(qr);
        List<FeedbackPair> feedbackPairs = new ArrayList<>();
        feedbackPairs.add(feedbackPair);
        feedbackPairs.add(feedbackPair2);
        QuestionData data = new QuestionData();
        data.setId("");
        data.setLessonId(super.lessonKey);
        
        data.setQuestionType(Question_FillInBlank_Input.QUESTION_TYPE);
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
        String phoneNumber = qr.phoneNumber;
        List<String> phoneNumberWords = StringUtils.convertPhoneNumberToPhoneNumberWords(phoneNumber);
        String phoneNumberWord = phoneNumberWords.get(0);
        String country = GrammarRules.definiteArticleBeforeCountry(qr.countryEN);

        String sentence1 = "The emergency phone number of " + country + " is " + phoneNumberWord + ".";

        String sentence2 = qr.countryJP + "の緊急通報用電話番号は" + Question_FillInBlank_Input.FILL_IN_BLANK_NUMBER +
                "です。";
        return sentence1 + "\n\n" + sentence2;

    }



    private String fillInBlankInputAnswer2(QueryResult qr){
        //no need to worry about non-digits (we removed all of them)
        return qr.phoneNumber;
    }

    private List<QuestionData> createFillInBlankInputQuestion2(QueryResult qr){
        String question = this.fillInBlankInputQuestion2(qr);

        String answer = fillInBlankInputAnswer2(qr);
        QuestionData data = new QuestionData();
        data.setId("");
        data.setLessonId(super.lessonKey);
        
        data.setQuestionType(Question_FillInBlank_Input.QUESTION_TYPE);
        data.setQuestion(question);
        data.setChoices(null);
        data.setAnswer(answer);
        data.setAcceptableAnswers(null);

        data.setFeedback(null);

        List<QuestionData> questionDataList = new ArrayList<>();
        questionDataList.add(data);
        return questionDataList;
    }

}