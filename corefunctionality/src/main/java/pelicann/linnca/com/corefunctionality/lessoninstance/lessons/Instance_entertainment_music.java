package pelicann.linnca.com.corefunctionality.lessoninstance.lessons;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;

import pelicann.linnca.com.corefunctionality.connectors.SPARQLDocumentParserHelper;
import pelicann.linnca.com.corefunctionality.connectors.WikiBaseEndpointConnector;
import pelicann.linnca.com.corefunctionality.connectors.WikiDataSPARQLConnector;
import pelicann.linnca.com.corefunctionality.lesson.lessons.Entertainment_music;
import pelicann.linnca.com.corefunctionality.lessoninstance.EntityPropertyData;
import pelicann.linnca.com.corefunctionality.lessoninstance.InstanceGenerator;
import pelicann.linnca.com.corefunctionality.lessoninstance.Translation;
import pelicann.linnca.com.corefunctionality.userinterests.WikiDataEntity;

public class Instance_entertainment_music extends InstanceGenerator {
    public Instance_entertainment_music(){
        super();
        super.lessonKey = Entertainment_music.KEY;
        super.uniqueEntities = 1;
    }

    @Override
    protected String getSPARQLQuery(){
        return "SELECT DISTINCT ?person ?personLabel ?personENLabel " +
                " ?songLabel ?songENLabel " +
                " ?genreLabel ?genreENLabel " +
                " ?artistLabel ?artistENLabel " +
                " ?picLabel " +
                "WHERE " +
                "{" +
                "    ?song   wdt:P31/wdt:P279  wd:Q7302866; " +
                "            wdt:P136         ?genre ;" +
                "            wdt:P175         ?person ;" +
                "            rdfs:label       ?songENLabel . " +
                "    ?person rdfs:label ?personENLabel . " +
                "    ?genre rdfs:label ?genreENLabel . " +
                "    OPTIONAL { ?person    wdt:P18    ?pic } . " + //image if possible
                "    FILTER (LANG(?personENLabel) = '" +
                WikiBaseEndpointConnector.ENGLISH + "') . " +
                "    FILTER (LANG(?songENLabel) = '" +
                WikiBaseEndpointConnector.ENGLISH + "') . " +
                "    FILTER (LANG(?genreENLabel) = '" +
                WikiBaseEndpointConnector.ENGLISH + "') . " +
                "    SERVICE wikibase:label { bd:serviceParam wikibase:language '" +
                WikiBaseEndpointConnector.LANGUAGE_PLACEHOLDER + "', '" + //JP label if possible
                WikiBaseEndpointConnector.ENGLISH + "'} . " + //fallback language is English
                "    BIND (wd:%s as ?person) . " + //binding the ID of entity as ?person
                "} ";

    }

    @Override
    protected synchronized void processResultsIntoEntityPropertyData(Document document){
        NodeList allResults = document.getElementsByTagName(
                WikiDataSPARQLConnector.RESULT_TAG
        );
        int resultLength = allResults.getLength();
        for (int i=0; i<resultLength; i++){
            Node head = allResults.item(i);
            String personID = SPARQLDocumentParserHelper.findValueByNodeName(head, "person");
            personID = WikiDataEntity.getWikiDataIDFromReturnedResult(personID);
            String personEN = SPARQLDocumentParserHelper.findValueByNodeName(head, "personENLabel");
            String personJP = SPARQLDocumentParserHelper.findValueByNodeName(head, "personLabel");
            Translation personTranslation = new Translation(personEN, personJP);
            String genreEN = SPARQLDocumentParserHelper.findValueByNodeName(head, "genreENLabel");
            String genreJP = SPARQLDocumentParserHelper.findValueByNodeName(head, "genreLabel");
            Translation genreTranslation = new Translation(genreEN, genreJP);
            String songEN = SPARQLDocumentParserHelper.findValueByNodeName(head, "songENLabel");
            String songJP = SPARQLDocumentParserHelper.findValueByNodeName(head, "songLabel");
            Translation songTranslation = new Translation(songEN, songJP);
            List<Translation> properties = new ArrayList<>();
            properties.add(personTranslation);
            properties.add(genreTranslation);
            properties.add(songTranslation);
            EntityPropertyData entityPropertyData = new EntityPropertyData();
            entityPropertyData.setLessonKey(lessonKey);
            entityPropertyData.setWikidataID(personID);
            entityPropertyData.setProperties(properties);

            String pic = SPARQLDocumentParserHelper.findValueByNodeName(head, "picLabel");
            if (pic != null && !pic.equals("")){
                pic = WikiDataSPARQLConnector.cleanImageURL(pic);
            }
            entityPropertyData.setImageURL(pic);

            newEntityPropertyData.add(entityPropertyData);
        }
    }
}
