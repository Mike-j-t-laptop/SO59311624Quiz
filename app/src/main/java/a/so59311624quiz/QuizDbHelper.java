package a.so59311624quiz;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.widget.CursorAdapter;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class QuizDbHelper extends SQLiteOpenHelper {

    public static final String DBNAME = "MyAwesomeQuiz.db";
    public static final int DBVERSION = 2;
    private static final String FK_ON_OPTIONS = " ON DELETE CASCADE ON UPDATE CASCADE";
    private static boolean onCreateRun = false;

    public QuizDbHelper(Context context) {
        super(context, DBNAME, null, DBVERSION);
        this.getWritableDatabase();
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        db.setForeignKeyConstraintsEnabled(true);
        super.onConfigure(db);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(Subject.CRT_SQL);
        db.execSQL(Chapter.CRT_SQL);
        db.execSQL(Question.CRT_SQL);
        db.execSQL(Answer.CRT_SQL);
        onCreateRun = true;
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    public long insertSubject(Subject subjectObject) {
        ContentValues cv = new ContentValues();
        if (subjectObject.getId() > 0) {
            cv.put(Subject.COL_ID,subjectObject.getId());
        }
        cv.put(Subject.COL_SUBJECT,subjectObject.getSubject());
        return this.getReadableDatabase().insert(Subject.TABLE_NAME,null,cv);
    }

    public static boolean wasDatabaseCreated() {
        return onCreateRun;
    }

    public long insertSubject(String subject) {
        return insertSubject(new Subject(subject));
    }

    public int deleteSubjectById(long id) {
        return this.getWritableDatabase()
                .delete(
                        Subject.TABLE_NAME,
                Subject.COL_ID + "=?",
                        new String[]{String.valueOf(id)}
                );
    }

    public long insertChapter(Chapter chapter) {
        ContentValues cv = new ContentValues();
        if (chapter.getId() > 0) {
            cv.put(Chapter.COL_ID,chapter.getId());
        }
        cv.put(Chapter.COL_SUBJECT_REFERENCE,chapter.getSubjectReference());
        cv.put(Chapter.COL_CHAPTER,chapter.getChapter());
        return this.getWritableDatabase().insert(Chapter.TABLE_NAME,null,cv);
    }

    public long insertChapter(String chapter, long subjectReference) {
        return insertChapter(new Chapter(subjectReference,chapter));
    }

    public long insertQuestion(Question question) {
        ContentValues cv = new ContentValues();
        if (question.getId() > 0) {
            cv.put(Question.COL_ID,question.getId());
        }
        cv.put(Question.COL_CHAPTER_REFERENCE,question.getChapterReferenced());
        cv.put(Question.COL_QUESTION,question.getQuestion());
        return this.getWritableDatabase().insert(Question.TABLE_NAME,null,cv);
    }

    public long insertAnswer(Answer answer) {
        ContentValues cv = new ContentValues();
        if (answer.getId() > 0) {
            cv.put(Answer.COL_ID,answer.getId());
        }
        cv.put(Answer.COl_QUESTION_REFERENCE, answer.getQuestionReferenced());
        cv.put(Answer.COL_ANSWER,answer.getAnswer());
        cv.put(Answer.COL_CORRECT_FLAG,answer.isCorrect_flag());
        return this.getWritableDatabase().insert(Answer.TABLE_NAME,null,cv);
    }

    public int insertQuestionAndAnswers(QuestionWithAnswers questionWithAnswers) {
        int rv = 0;
        SQLiteDatabase db = this.getWritableDatabase();
        long currentQuestionid;
        db.beginTransaction();
        if ((currentQuestionid = insertQuestion(questionWithAnswers.getQuestion())) > 0) {
            for (Answer a: questionWithAnswers.getAnswers()) {
                a.setQuestionReferenced(currentQuestionid);
                if (insertAnswer(a) > 0) {
                    rv++;
                }
            }
            db.setTransactionSuccessful();
        }
        db.endTransaction();
        return rv;
    }

    public Cursor getAllSubjects() {
        return this.getWritableDatabase().query(
                Subject.TABLE_NAME,
                null,null,null,null,null,null
        );
    }

    public Cursor getAllChaptersPerSubject(long subjectId) {
        String whereclause = Chapter.COL_SUBJECT_REFERENCE + "=?";
        String[] wheresrags = new String[]{String.valueOf(subjectId)};
        return this.getWritableDatabase().query(
                Chapter.TABLE_NAME,
                null,
                whereclause,
                wheresrags,
                null,null,null
        );
    }

    public String getQuestionsAndAnswerBySubjectAndChapter(long subjectId, long chapterId) {

        /*
        SELECT subject, chapter, question, group_concat(answer,'    ') AS answers
        FROM chapter
            JOIN subject ON subject._id = chapter.subject_reference
            JOIN question ON question.chapter_reference = chapter._id
            JOIN answer ON answer.question_reference = question._id
        WHERE
            subject._id = 1
                AND chapter._id = 1
        GROUP BY question._id
        ORDER BY chapter._id,question._id
                ;
         */

        String derivedColumn = "answers";
        String[] columns = new String[]{
                Subject.COL_SUBJECT,
                Chapter.COL_CHAPTER,
                Question.COL_QUESTION,
                "group_concat(" + Answer.COL_ANSWER + ",'/n/t') AS " + derivedColumn
        };

        String fromClause = Chapter.TABLE_NAME +
                " JOIN " + Subject.TABLE_NAME +
                " ON " + Subject.TABLE_NAME + "." + Subject.COL_ID +
                " = " +
                Chapter.TABLE_NAME + "." + Chapter.COL_SUBJECT_REFERENCE +

                " JOIN " + Question.TABLE_NAME +
                " ON " + Question.TABLE_NAME + "." + Question.COL_CHAPTER_REFERENCE +
                " = " +
                Chapter.TABLE_NAME + "." + Chapter.COL_ID +

                " JOIN " + Answer.TABLE_NAME +
                " ON " + Answer.TABLE_NAME + "." + Answer.COl_QUESTION_REFERENCE +
                " = " + Question.TABLE_NAME + "." + Question.COL_ID;

        String whereclause = Subject.TABLE_NAME + "." + Subject.COL_ID + "=?" +
                " AND " + Chapter.TABLE_NAME + "." + Chapter.COL_ID + "=?";
        String[] whereargs = new String[]{String.valueOf(subjectId),String.valueOf(chapterId)};

        Cursor csr = this.getWritableDatabase().query(
                fromClause,
                columns,
                whereclause,
                whereargs,
                Question.TABLE_NAME + "." + Question.COL_ID,
                null,
                Chapter.TABLE_NAME + "." + Chapter.COL_ID + "," + Question.TABLE_NAME + "." + Question.COL_ID
        );
        StringBuilder sb = new StringBuilder();
        while (csr.moveToNext()) {
            if (sb.length() < 1) {
                sb.append("Subject is ").append(csr.getString(csr.getColumnIndex(Subject.COL_SUBJECT)));
                sb.append(" - Chapter is ").append(csr.getString(csr.getColumnIndex(Chapter.COL_CHAPTER)));
            }
            sb.append("\n").append(csr.getString(csr.getColumnIndex(Question.COL_QUESTION)));
            sb.append("\n\t").append(csr.getString(csr.getColumnIndex(derivedColumn)));
        }
        csr.close();
        return sb.toString();
    }

    public class Subject {

        public static final String TABLE_NAME = "subject";
        public static final String COL_ID = BaseColumns._ID;
        public static final String COL_SUBJECT = "subject";
        private static final String CRT_SQL = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME +
                "(" +
                COL_ID + " INTEGER PRIMARY KEY NOT NULL," +
                COL_SUBJECT + " TEXT UNIQUE" +
                ")";
        private long id;
        private String subject;



        // Table Classes Constants
        public Subject(){}

        public Subject(long id, String subject) {
            this.id = id;
            this.subject = subject;
        }

        public Subject(String subject) {
            this(-1,subject);
        }

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        public String getSubject() {
            return subject;
        }

        public void setSubject(String subject) {
            this.subject = subject;
        }
    }

    public class Chapter {

        public static final String TABLE_NAME = "chapter";
        public static final String COL_ID = BaseColumns._ID;
        public static final String COL_SUBJECT_REFERENCE = "subject_reference";
        public static final String COL_CHAPTER = "chapter";
        private static final String CRT_SQL = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME +
                "(" +
                COL_ID + " INTEGER PRIMARY KEY NOT NULL," +
                COL_SUBJECT_REFERENCE + " INTEGER NOT NULL " +
                "REFERENCES " +
                Subject.TABLE_NAME + "(" +
                Subject.COL_ID +
                ") " + FK_ON_OPTIONS + "," +
                COL_CHAPTER + " TEXT UNIQUE" +
                ")";

        private long id;
        private long subjectReference;
        private String chapter;

        public Chapter(){}

        public Chapter(long id, long subjectReferenced, String chapter) {
            this.id = id;
            this.subjectReference = subjectReferenced;
            this.chapter = chapter;
        }

        public  Chapter(long subjectReferenced, String chapter) {
            this(-1,subjectReferenced,chapter);
        }

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        public long getSubjectReference() {
            return subjectReference;
        }

        public void setSubjectReference(long subjectReference) {
            this.subjectReference = subjectReference;
        }

        public String getChapter() {
            return chapter;
        }

        public void setChapter(String chapter) {
            this.chapter = chapter;
        }
    }

    public class Answer {

        public static final String TABLE_NAME = "answer";
        public static final String COL_ID = BaseColumns._ID;
        public static final String COl_QUESTION_REFERENCE = "question_reference";
        public static final String COL_ANSWER = "answer";
        public static final String COL_CORRECT_FLAG = "correct_flag";
        private static final String CRT_SQL = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME +
                "(" +
                COL_ID + " INTEGER PRIMARY KEY NOT NULL," +
                COl_QUESTION_REFERENCE + " INTEGER NOT NULL " +
                "REFERENCES " + Question.TABLE_NAME + "(" +
                Question.COL_ID +
                ")" + FK_ON_OPTIONS + ", " +
                COL_ANSWER + " TEXT, " +
                COL_CORRECT_FLAG + " INTEGER " +
                ")";

        private long id;
        private long questionReferenced;
        private String answer;
        private boolean correct_flag;

        public Answer(){}

        public Answer(long id, long questionReferenced, String answer, boolean correct_flag) {
            this.id = id;
            this.questionReferenced = questionReferenced;
            this.answer = answer;
            this.correct_flag = correct_flag;
        }

        public Answer(long questionReferenced, String answer, boolean correct_flag) {
            this(-1,questionReferenced,answer,correct_flag);
        }

        public Answer(long questionReferenced, String answer) {
            this(questionReferenced,answer,false);
        }

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        public long getQuestionReferenced() {
            return questionReferenced;
        }

        public void setQuestionReferenced(long questionReferenced) {
            this.questionReferenced = questionReferenced;
        }

        public String getAnswer() {
            return answer;
        }

        public void setAnswer(String answer) {
            this.answer = answer;
        }

        public boolean isCorrect_flag() {
            return correct_flag;
        }

        public void setCorrect_flag(boolean correct_flag) {
            this.correct_flag = correct_flag;
        }
    }

    public class Question {

        public static final String TABLE_NAME = "question";
        public static final String COL_ID = BaseColumns._ID;
        public static final String COL_CHAPTER_REFERENCE = "chapter_reference";
        public static final String COL_QUESTION = "question";
        private static final String CRT_SQL = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + "(" +
                COL_ID + " INTEGER PRIMARY KEY NOT NULL," +
                COL_CHAPTER_REFERENCE + " INTEGER NOT NULL" +
                " REFERENCES " + Chapter.TABLE_NAME + "(" +
                Chapter.COL_ID +
                ")" +
                FK_ON_OPTIONS + "," +
                COL_QUESTION + " TEXT UNIQUE " +
                ")";

        private long id;
        private long chapterReferenced;
        private String question;

        public Question(){}

        public Question(long id, long chapterReferenced, String question) {
            this.id = id;
            this.chapterReferenced = chapterReferenced;
            this.question = question;
        }

        public Question(long chapterReferenced, String question) {
            this(-1,chapterReferenced,question);
        }

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        public long getChapterReferenced() {
            return chapterReferenced;
        }

        public void setChapterReferenced(long chapterReferenced) {
            this.chapterReferenced = chapterReferenced;
        }

        public String getQuestion() {
            return question;
        }

        public void setQuestion(String question) {
            this.question = question;
        }
    }

    public class QuestionWithAnswers {
        private Question question;
        private ArrayList<Answer> answers = new ArrayList<>();

        public QuestionWithAnswers(Question question, List<Answer> answers) {
            this.question = question;
            this.answers = new ArrayList<>(answers);
        }

        public void setQuestion(Question question) {
            this.question = question;
        }

        public Question getQuestion() {
            return question;
        }

        public void setAnswers(ArrayList<Answer> answers) {
            this.answers = answers;
        }

        public ArrayList<Answer> getAnswers() {
            return answers;
        }

        public void addAnswer(Answer answer) {
            this.answers.add(answer);
        }
    }

    public void addData() {
        long currentSubject;
        long currentChapter;
        String question = "";
        ArrayList<Answer> answers = new ArrayList<>();
        if ((currentSubject =insertSubject("Current Affairs")) > 0) {
            if ((currentChapter = insertChapter("CA Chapter1",currentSubject)) > 0) {
                question = "The worlds oldest international human rights organization is?";
                answers.clear();
                answers.add(new Answer(-1,"amnesty international",false));
                answers.add(new Answer(-1,"freedom house",false));
                answers.add(new Answer(-1,"anti slavery",true));
                answers.add(new Answer(-1,"non of these",false));
                this.insertQuestionAndAnswers(new QuestionWithAnswers(
                        new Question(currentChapter,question),answers));

                question = "The constitution of European union has not been ratified by?";
                answers.clear();
                answers.add(new Answer(-1,"Italy",false));
                answers.add(new Answer(-1,"Netherlands",false));
                answers.add(new Answer(-1,"France",true));
                answers.add(new Answer(-1,"non of these",false));
                this.insertQuestionAndAnswers(new QuestionWithAnswers(
                        new Question(currentChapter,question),answers));

                question = "After united states, the largest contributor in the united nations budget is?";
                answers.clear();
                answers.add(new Answer(-1,"Germany",false));
                answers.add(new Answer(-1,"UK",true));
                answers.add(new Answer(-1,"France",false));
                answers.add(new Answer(-1,"non of these",false));
                this.insertQuestionAndAnswers(new QuestionWithAnswers(
                        new Question(currentChapter,question),answers));
            }

            if ((currentChapter = insertChapter("CA Chapter2",currentSubject)) > 0) {
                question = "The worlds newest international human rights organization is?";
                answers.clear();
                answers.add(new Answer(-1,"amnesty international",false));
                answers.add(new Answer(-1,"freedom house",false));
                answers.add(new Answer(-1,"anti slavery",false));
                answers.add(new Answer(-1,"non of these",true));
                this.insertQuestionAndAnswers(new QuestionWithAnswers(
                        new Question(currentChapter,question),answers));

                question = "The country not in the European Union is?";
                answers.clear();
                answers.add(new Answer(-1,"Italy",false));
                answers.add(new Answer(-1,"Netherlands",false));
                answers.add(new Answer(-1,"France",false));
                answers.add(new Answer(-1,"non of these",true));
                this.insertQuestionAndAnswers(new QuestionWithAnswers(
                        new Question(currentChapter,question),answers));

                question = "After united states, the smallest contributor in the united nations budget is?";
                answers.clear();
                answers.add(new Answer(-1,"Germany",false));
                answers.add(new Answer(-1,"UK",false));
                answers.add(new Answer(-1,"France",false));
                answers.add(new Answer(-1,"non of these",true));
                this.insertQuestionAndAnswers(new QuestionWithAnswers(
                        new Question(currentChapter,question),answers));
            }
        }
        if ((currentSubject =insertSubject("Other Stuff")) > 0) {
            if ((currentChapter = insertChapter("OS Chapter1",currentSubject)) > 0) {
                question = "Noddy's friend was?";
                answers.clear();
                answers.add(new Answer(-1,"Fred",false));
                answers.add(new Answer(-1,"Big Ears",true));
                answers.add(new Answer(-1,"Weed",false));
                answers.add(new Answer(-1,"Postman Pat",false));
                this.insertQuestionAndAnswers(new QuestionWithAnswers(
                        new Question(currentChapter,question),answers));

                question = "Bob the builder, built what?";
                answers.clear();
                answers.add(new Answer(-1,"Factories",false));
                answers.add(new Answer(-1,"Houses",true));
                answers.add(new Answer(-1,"Universes",false));
                answers.add(new Answer(-1,"Space Ships",false));
                this.insertQuestionAndAnswers(new QuestionWithAnswers(
                        new Question(currentChapter,question),answers));

                question = "King Alfred was born in?";
                answers.clear();
                answers.add(new Answer(-1,"Oxford",false));
                answers.add(new Answer(-1,"Londinium",false));
                answers.add(new Answer(-1,"Glasgow",false));
                answers.add(new Answer(-1,"Wantage",true));
                this.insertQuestionAndAnswers(new QuestionWithAnswers(
                        new Question(currentChapter,question),answers));
            }
            if ((currentChapter = insertChapter("OS Chapter2",currentSubject)) > 0) {
                question = "Who was Dr Who?";
                answers.clear();
                answers.add(new Answer(-1, "A Dalek", false));
                answers.add(new Answer(-1, "A Fool", false));
                answers.add(new Answer(-1, "An Astronaut", false));
                answers.add(new Answer(-1, "A Time Lord", true));
                this.insertQuestionAndAnswers(new QuestionWithAnswers(
                        new Question(currentChapter, question), answers));

                question = "Led Zepplin's Whole Lotta ?";
                answers.clear();
                answers.add(new Answer(-1, "Money", false));
                answers.add(new Answer(-1, "Love", true));
                answers.add(new Answer(-1, "Hate", false));
                answers.add(new Answer(-1, "Food", false));
                this.insertQuestionAndAnswers(new QuestionWithAnswers(
                        new Question(currentChapter, question), answers));

                question = "Double Barrel was a hit single by?";
                answers.clear();
                answers.add(new Answer(-1, "Peter, Paul and Mary", false));
                answers.add(new Answer(-1, "Lindisfarne", false));
                answers.add(new Answer(-1, "Dave Lee Travis", false));
                answers.add(new Answer(-1, "Dave and Ansill Collins", true));
                this.insertQuestionAndAnswers(new QuestionWithAnswers(
                        new Question(currentChapter, question), answers));
            }
        }
    }
}
