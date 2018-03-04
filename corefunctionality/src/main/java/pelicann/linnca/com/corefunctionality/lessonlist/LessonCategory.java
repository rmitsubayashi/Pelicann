package pelicann.linnca.com.corefunctionality.lessonlist;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class LessonCategory implements Serializable{
    private String key;
    private String titleJP;
    private List<String> lessonKeys = new ArrayList<>();

    public LessonCategory() {
    }

    public LessonCategory(String key, String titleJP, List<String> lessonKeys) {
        this.key = key;
        this.titleJP = titleJP;
        this.lessonKeys = lessonKeys;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getTitleJP() {
        return titleJP;
    }

    public void setTitleJP(String titleJP) {
        this.titleJP = titleJP;
    }

    public void addLessonKey(String lessonKey){
        this.lessonKeys.add(lessonKey);
    }

    public boolean hasLesson(String lessonKey){
        return this.lessonKeys.contains(lessonKey);
    }

    public String getLessonKey(int index){
        if (index >= lessonKeys.size()){
            System.out.println("lesson index out of bounds");
            return null;
        }
        return lessonKeys.get(index);
    }

    public int getLessonCount(){
        return lessonKeys.size();
    }
}