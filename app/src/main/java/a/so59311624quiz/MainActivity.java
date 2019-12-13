package a.so59311624quiz;

import androidx.appcompat.app.AppCompatActivity;

import android.database.Cursor;
import android.database.DatabaseUtils;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    QuizDbHelper mQDBHelper;
    Spinner mSubject,mChapter;
    TextView mQuestions;
    SimpleCursorAdapter mSubjectAdapter,mChapterAdapter;
    Cursor mSubjectCursor,mChapterCursor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mSubject = this.findViewById(R.id.subject_selection);
        mChapter = this.findViewById(R.id.chapter_selection);
        mQuestions = this.findViewById(R.id.questions);

        mQDBHelper = new QuizDbHelper(this);
        if (QuizDbHelper.wasDatabaseCreated()) {
            mQDBHelper.addData();
        }
        Cursor csr = mQDBHelper.getWritableDatabase().query("sqlite_master",null,null,null,null,null,null);
        DatabaseUtils.dumpCursor(csr);
        manageSubjectSpinner();
    }

    private void manageSubjectSpinner() {
        mSubjectCursor = mQDBHelper.getAllSubjects();
        if (mSubjectAdapter == null) {
            mSubjectAdapter = new SimpleCursorAdapter(
                    this,
                    android.R.layout.simple_list_item_1,
                    mSubjectCursor,
                    new String[]{QuizDbHelper.Subject.COL_SUBJECT},
                    new int[]{android.R.id.text1},
                    0
            );
            mSubject.setAdapter(mSubjectAdapter);
            mSubject.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    manageChapterSpinner();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });
        } else {
            mSubjectAdapter.swapCursor(mSubjectCursor);
        }
    }

    private void manageChapterSpinner() {
        mChapterCursor = mQDBHelper.getAllChaptersPerSubject(mSubject.getSelectedItemId());
        if (mChapterAdapter == null) {
            mChapterAdapter = new SimpleCursorAdapter(
                    this,
                    android.R.layout.simple_list_item_1,
                    mChapterCursor,
                    new String[]{QuizDbHelper.Chapter.COL_CHAPTER},
                    new int[]{android.R.id.text1},
                    0
            );
            mChapter.setAdapter(mChapterAdapter);
            mChapter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    manageQuestionText();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });
        } else {
            mChapterAdapter.swapCursor(mChapterCursor);
        }
    }

    private void manageQuestionText() {
        mQuestions.setText(
                mQDBHelper.getQuestionsAndAnswerBySubjectAndChapter(
                        mSubject.getSelectedItemId(),
                        mChapter.getSelectedItemId()
                )
        );
    }
}
