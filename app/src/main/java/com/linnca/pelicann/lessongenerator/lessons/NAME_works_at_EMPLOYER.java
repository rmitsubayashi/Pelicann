package com.linnca.pelicann.lessongenerator.lessons;

import com.linnca.pelicann.connectors.SPARQLDocumentParserHelper;
import com.linnca.pelicann.connectors.WikiBaseEndpointConnector;
import com.linnca.pelicann.connectors.WikiDataSPARQLConnector;
import com.linnca.pelicann.lessongenerator.GrammarRules;
import com.linnca.pelicann.lessongenerator.Lesson;
import com.linnca.pelicann.lessongenerator.LessonGeneratorUtils;
import com.linnca.pelicann.questions.QuestionData;
import com.linnca.pelicann.questions.QuestionDataWrapper;
import com.linnca.pelicann.questions.QuestionTypeMappings;
import com.linnca.pelicann.questions.QuestionUtils;
import com.linnca.pelicann.questions.Question_FillInBlank_Input;
import com.linnca.pelicann.questions.Question_FillInBlank_MultipleChoice;
import com.linnca.pelicann.userinterests.WikiDataEntryData;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NAME_works_at_EMPLOYER extends Lesson {
    public static final String KEY = "NAME_works_at_EMPLOYER";

    private final List<QueryResult> queryResults = new ArrayList<>();
    private final Map<String, List<String>> queryResultMap = new HashMap<>();

    private class QueryResult {
        private final String personID;
        private final String personEN;
        private final String personJP;
        private final String employerID;
        private final String employerEN;
        private final String employerJP;

        private QueryResult(
                String personID,
                String personEN,
                String personJP,
                String employerID,
                String employerEN,
                String employerJP)
        {
            this.personID = personID;
            this.personEN = personEN;
            this.personJP = personJP;
            this.employerID = employerID;
            this.employerEN = employerEN;
            this.employerJP = employerJP;
        }
    }

    public NAME_works_at_EMPLOYER(WikiBaseEndpointConnector connector, LessonListener listener){
        super(connector, listener);
        super.questionSetsLeftToPopulate = 2;
        super.categoryOfQuestion = WikiDataEntryData.CLASSIFICATION_PERSON;
        super.lessonKey = KEY;

    }

    @Override
    protected String getSPARQLQuery(){
        //since there aren't that many Japanese employers available,
        //just get the employer name and convert it to a employer by adding "~人"
        return "SELECT ?person ?personLabel ?personEN " +
                " ?employer ?employerEN ?employerLabel " +
                "WHERE " +
                "{" +
                "    {?person wdt:P31 wd:Q5} UNION " + //is human
                "    {?person wdt:P31 wd:Q15632617} ." + //or fictional human
                "    ?person wdt:P108 ?employer . " + //has an employer
                "    ?person rdfs:label ?personEN . " + //English label
                "    ?employer rdfs:label ?employerEN . " + //English label
                "    FILTER (LANG(?employerEN) = '" +
                WikiBaseEndpointConnector.ENGLISH + "') . " +
                "    FILTER (LANG(?personEN) = '" +
                WikiBaseEndpointConnector.ENGLISH + "') . " +
                "    SERVICE wikibase:label {bd:serviceParam wikibase:language '" +
                WikiBaseEndpointConnector.LANGUAGE_PLACEHOLDER + "','" +
                WikiBaseEndpointConnector.ENGLISH + "' } " +
                "    BIND (wd:%s as ?person) . " + //binding the ID of entity as ?person
                "} ";

    }

    @Override
    protected void processResultsIntoClassWrappers(Document document) {
        NodeList allResults = document.getElementsByTagName(
                WikiDataSPARQLConnector.RESULT_TAG
        );
        int resultLength = allResults.getLength();
        for (int i=0; i<resultLength; i++){
            Node head = allResults.item(i);
            String personID = SPARQLDocumentParserHelper.findValueByNodeName(head, "person");
            personID = LessonGeneratorUtils.stripWikidataID(personID);
            String personEN = SPARQLDocumentParserHelper.findValueByNodeName(head, "personEN");
            String personJP = SPARQLDocumentParserHelper.findValueByNodeName(head, "personLabel");
            String employerID = SPARQLDocumentParserHelper.findValueByNodeName(head, "employerName");
            employerID = LessonGeneratorUtils.stripWikidataID(employerID);
            String employerJP = SPARQLDocumentParserHelper.findValueByNodeName(head, "employerLabel");
            String employerEN = SPARQLDocumentParserHelper.findValueByNodeName(head, "employerEN");

            //to help with multiple choice questions
            if (queryResultMap.containsKey(personID)){
                List<String> value = queryResultMap.get(personID);
                value.add(employerID);
            } else {
                List<String> list = new ArrayList<>();
                list.add(employerID);
                queryResultMap.put(personID, list);
            }

            QueryResult qr = new QueryResult(personID, personEN, personJP, employerID, employerEN, employerJP);
            queryResults.add(qr);
        }
    }

    @Override
    protected int getQueryResultCt(){ return queryResults.size(); }

    @Override
    protected void createQuestionsFromResults(){
        for (QueryResult qr : queryResults){
            List<List<QuestionData>> questionSet = new ArrayList<>();
            List<QuestionData> fillInBlankMultipleChoiceQuestion = createFillInBlankMultipleChoiceQuestion(qr);
            questionSet.add(fillInBlankMultipleChoiceQuestion);

            List<QuestionData> fillInBlankQuestion = createFillInBlankQuestion(qr);
            questionSet.add(fillInBlankQuestion);

            super.newQuestions.add(new QuestionDataWrapper(questionSet, qr.personID, qr.personJP, null));
        }

    }

    /* Note that some of these employers may need the article 'the' before it.
     * We can't guarantee that all of them will be accurate...
     * Just make sure to let the user be aware that there may be some mistakes
     * */
    private String NAME_works_at_EMPLOYER_EN_correct(QueryResult qr){
        //use the definite article before school name ( ~ of ~)
        //for better accuracy.
        //still there are a lot of employers that will need 'the'
        String sentence = qr.personEN + " works at " +
                GrammarRules.definiteArticleBeforeSchoolName(qr.employerEN) + ".";
        //no need since all names are capitalized?
        sentence = GrammarRules.uppercaseFirstLetterOfSentence(sentence);
        return sentence;
    }

    private String formatSentenceJP(QueryResult qr){
        return qr.personJP + "は" + qr.employerJP + "で働いています。";
    }

    private String fillInBlankMultipleChoiceQuestion(QueryResult qr){
        String sentence = qr.personEN + " works at " + Question_FillInBlank_MultipleChoice.FILL_IN_BLANK_MULTIPLE_CHOICE + ".";
        return GrammarRules.uppercaseFirstLetterOfSentence(sentence);
    }

    private List<QueryResult> bigEmployers(){
        List<QueryResult> employers = new ArrayList<>(5);
        employers.add(new QueryResult("","","","Q53268","Toyota","トヨタ自動車"));
        employers.add(new QueryResult("","","","Q201653","SoftBank", "ソフトバンクグループ株式会社"));
        employers.add(new QueryResult("","","","Q41187","Sony","ソニー"));
        employers.add(new QueryResult("","","","Q95","Google","Google"));
        employers.add(new QueryResult("","","","Q312","Apple Inc.","アップル"));
        return employers;
    }

    private String fillInBlankMultipleChoiceAnswer(QueryResult qr){
        return GrammarRules.definiteArticleBeforeSchoolName(qr.employerEN);
    }

    private List<String> fillInBlankMultipleChoiceChoices(QueryResult qr){
        List<String> allEmployers = queryResultMap.get(qr.personID);
        List<QueryResult> possibleEmployers = bigEmployers();
        for (Iterator<QueryResult> iterator = possibleEmployers.iterator(); iterator.hasNext();){
            QueryResult employer = iterator.next();
            if (allEmployers.contains(employer.employerID)){
                iterator.remove();
            }
        }

        if (possibleEmployers.size() > 2){
            Collections.shuffle(possibleEmployers);
            possibleEmployers = possibleEmployers.subList(0,2);
        }

        List<String> choices = new ArrayList<>(2);
        for (QueryResult employer : possibleEmployers){
            choices.add(employer.employerEN);
        }
        return choices;
    }

    private List<QuestionData> createFillInBlankMultipleChoiceQuestion(QueryResult qr){
        String question = this.fillInBlankMultipleChoiceQuestion(qr);
        String answer = fillInBlankMultipleChoiceAnswer(qr);
        List<QuestionData> questionDataList = new ArrayList<>();
        List<String> choices = fillInBlankMultipleChoiceChoices(qr);
        choices.add(answer);
        QuestionData data = new QuestionData();
        data.setId("");
        data.setLessonId(lessonKey);
        data.setTopic(qr.personJP);
        data.setQuestionType(QuestionTypeMappings.FILL_IN_BLANK_MULTIPLE_CHOICE);
        data.setQuestion(question);
        data.setChoices(choices);
        data.setAnswer(answer);
        data.setAcceptableAnswers(null);


        questionDataList.add(data);

        return questionDataList;
    }

    private String fillInBlankQuestion(QueryResult qr){
        String sentence = formatSentenceJP(qr);
        String sentence2 = qr.personEN + " " + Question_FillInBlank_Input.FILL_IN_BLANK_TEXT +
                " " + qr.employerEN + ".";
        sentence2 = GrammarRules.uppercaseFirstLetterOfSentence(sentence2);
        return sentence + "\n\n" + sentence2;
    }

    private String fillInBlankAnswer(){
        return "works at";
    }

    private List<String> fillInBlankAcceptableAnswers(){
        List<String> acceptableAnswers = new ArrayList<>(1);
        acceptableAnswers.add("work at");
        return acceptableAnswers;
    }

    private List<QuestionData> createFillInBlankQuestion(QueryResult qr){
        String question = this.fillInBlankQuestion(qr);
        String answer = fillInBlankAnswer();
        List<String> acceptableAnswers = fillInBlankAcceptableAnswers();
        List<QuestionData> questionDataList = new ArrayList<>();
        QuestionData data = new QuestionData();
        data.setId("");
        data.setLessonId(lessonKey);
        data.setTopic(qr.personJP);
        data.setQuestionType(QuestionTypeMappings.FILL_IN_BLANK_INPUT);
        data.setQuestion(question);
        data.setChoices(null);
        data.setAnswer(answer);
        data.setAcceptableAnswers(acceptableAnswers);


        questionDataList.add(data);

        return questionDataList;
    }



    //TODO preposition question
}