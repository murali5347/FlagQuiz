package com.example.murali.flaggame;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.print.PrintHelper;
import android.support.v7.app.AlertDialog;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends android.app.Fragment{

    private static final String TAG = "FlagQuiz Activity";

    private static final int FLAG_IN_QUIZ = 10;

    private List<String> fileNameList; //flag file name
    private List<String> quizCountriesList; // countries in the current quiz
    private Set<String> regionSet; // world regions in current quiz
    private String correctAnswer; // correct country for the current flag
    private int totalGuesses; // number of guesses made
    private int correctAnswers; // number of correct guesses
    private int guessRows; // number of rows displaying guess buttons
    private SecureRandom random; // used to randomaize the quiz
    private Handler handler; // used to delay loading next flag
    private Animation shakeAnimation; // animation for incorrect guess


    private LinearLayout quizLinearLayout; // layout that contains the quiz
    private TextView questionNumberTextView; // shows current question
    private ImageView flagImageView; // displays a flag
    private LinearLayout[] guessLinearLayouts; // rows of answer Buttons
    private TextView answerTextView; // displays correct answer

    //configures the MainActivityFragment when its View is created
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_main, container, false);

        fileNameList = new ArrayList<>();
        quizCountriesList = new ArrayList<>();
        random = new SecureRandom();
        handler = new Handler();


        //Load the shake animation thats used for incorrect answers

        shakeAnimation = AnimationUtils.loadAnimation(getActivity(), R.anim.incorrect_shake);
        shakeAnimation.setRepeatCount(3);


        //get the reference of the GUI components

        quizLinearLayout = (LinearLayout) view.findViewById(R.id.quizLinearLayout);
        questionNumberTextView = (TextView) view.findViewById(R.id.questionNumberTextView);
        flagImageView = (ImageView) view.findViewById(R.id.flagImageView);
        answerTextView = (TextView) view.findViewById(R.id.answerTextView);
        guessLinearLayouts = new LinearLayout[4];
        guessLinearLayouts[0] = (LinearLayout) view.findViewById(R.id.row1LinearLayout);
        guessLinearLayouts[1] = (LinearLayout) view.findViewById(R.id.row2LinearLayout);
        guessLinearLayouts[2] = (LinearLayout) view.findViewById(R.id.row3LinearLayout);
        guessLinearLayouts[3] = (LinearLayout) view.findViewById(R.id.row4LinearLayout);


        //configure Listeners for the guess Buttons

        for (LinearLayout row : guessLinearLayouts) {
            for (int column = 0; column < row.getChildCount(); column++) {
                Button button = (Button) row.getChildAt(column);
                button.setOnClickListener(guessButtonListener);
            }
        }

        //set questionNumberTextView's text
        questionNumberTextView.setText(getString(R.string.question, 1, FLAG_IN_QUIZ));
        return view;

    }

    public void updateGuessRows(SharedPreferences sharedPreferences) {
        String choices = sharedPreferences.getString(MainActivity.CHOICES, null);
        guessRows = Integer.parseInt(choices) / 2;

        for (LinearLayout layout : guessLinearLayouts)
            layout.setVisibility(View.GONE);

        for (int row = 0; row < guessRows; row++) {
            guessLinearLayouts[row].setVisibility(View.VISIBLE);
        }
    }
    //update world regions for quiz based on values in SharedPreferences

    public void updateRegions(SharedPreferences sharedPreferences) {
        regionSet = sharedPreferences.getStringSet(MainActivity.REGIONS, null);
    }

    public void resetQuiz() {
        //use assetManager to get image file names for enabled regions
        AssetManager assets = getActivity().getAssets();
        fileNameList.clear();//empty list of image file names

        try {
            //loop through each region
            for (String region : regionSet) {
                //get a list of all flag image files in this region
                String[] paths = assets.list(region);
                for (String path : paths) {
                    fileNameList.add(path.replace(".png", ""));
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "error loading in images file names", e);
        }

        correctAnswers = 0;//reset the number of crrect answers made
        totalGuesses = 0; // reset the total number of guesses the user made
        quizCountriesList.clear(); // clear prior list of quiz countries


        int FlagCounter = 1;
        int numberOfFlags = fileNameList.size();

        //add FLAGS_IN_QUIZ random file names to the quizCountriesList

        while (FlagCounter <= FLAG_IN_QUIZ) {

            int randomIndex = random.nextInt(numberOfFlags);

            //get the random file name
            String filename = fileNameList.get(randomIndex);

            //if the region is enabled and it hasn't already been chosen

            if (!quizCountriesList.contains(filename)) {
                quizCountriesList.add(filename);
                ++FlagCounter;
            }

        }
        loadNextFlag();

    }
//after the usr guesses a correct flag, load the next flag

    private void loadNextFlag() {
        //get file name of the next flag and remove it from the list
        String nextImage = quizCountriesList.remove(0);
        correctAnswer = nextImage; //update the correct answer
        answerTextView.setText(""); // clear answerTextView

        //display current question number
        questionNumberTextView.setText(getString(R.string.question, (correctAnswers + 1), FLAG_IN_QUIZ));

        //extract the region from the next Image's name
        String region = nextImage.substring(0, nextImage.indexOf('-'));

        //use AssetManager to load next image from assets folder
        AssetManager assets = getActivity().getAssets();

        //get an imputStream to the asset representing the next flag
        //and try to use the inputstream

        try (InputStream stream = assets.open(region + "/" + nextImage + ".png")) {
            //load the assets as a drawable and display on the flagimageView

            Drawable flag = Drawable.createFromStream(stream, nextImage);
            flagImageView.setImageDrawable(flag);

            animate(false); // animate the flag onto the screen
        } catch (IOException e) {
            Log.e(TAG, "error loading" + nextImage, e);
        }

        Collections.shuffle(fileNameList); // shuffle file names

        //put the correct answer at the end of the fileNameList

        int correct = fileNameList.indexOf(correctAnswer);
        fileNameList.add(fileNameList.remove(correct));


        //add 2,4,6 or 8 guess buttons based on the value of guesss rows

        for (int rows = 0; rows < guessRows; rows++) {
            //place Buttons inn currentTableRow

            for (int column = 0; column < guessLinearLayouts[rows].getChildCount(); column++) {

                //get reference to button to configure


                Button newGuessButton = (Button) guessLinearLayouts[rows].getChildAt(column);
                newGuessButton.setEnabled(true);


                //GET COUNTRY NAME AND SET IT AS newGuessButtons' text

                String filename = fileNameList.get(rows * 2) + column;
                newGuessButton.setText(getCountryName(filename));


            }
        }


        //randomly replace one button with the correct answer

        int row = random.nextInt(guessRows);//pick random row
        int column = random.nextInt(2); //pick random column
        LinearLayout randomRow = guessLinearLayouts[row];//get the row
        String countryName = getCountryName(correctAnswer);
        ((Button) randomRow.getChildAt(column)).setText(countryName);
    }

    //parses the country flag file name an returns the country name

    private String getCountryName(String name) {
        return name.substring(name.indexOf('-') + 1).replace('_', ' ');

    }

    //animates the entire quizLinearLayout on or off screen
    private void animate(boolean animateOut) {
        //prevent animation into the UI for the first flag
        if (correctAnswers == 0)
            return;


        //calculate the center X and center Y
        int centerX = (quizLinearLayout.getLeft() + quizLinearLayout.getRight());
        int centerY = (quizLinearLayout.getTop() + quizLinearLayout.getBottom());

        //calculate the radius
        int radius = Math.max(quizLinearLayout.getWidth(), quizLinearLayout.getHeight());


        Animator animator;

        //if the quizLinearLayout should animate out rather than in
        if (animateOut) {
            //create circular reveal animation

            animator = ViewAnimationUtils.createCircularReveal(quizLinearLayout, centerX, centerY, radius, 0);
            animator.addListener(new AnimatorListenerAdapter() {
                                     @Override
                                     public void onAnimationEnd(Animator animation) {
                                         loadNextFlag();
                                     }
                                 }


            );

        } else { //if the quizlayout should animate in
            animator = ViewAnimationUtils.createCircularReveal(quizLinearLayout, centerX, centerY, 0, radius);

        }
        animator.setDuration(500); // set animation duration to 500ms
        animator.start();
    }

// called when a guess Button is touched

    private View.OnClickListener guessButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Button guessButton = ((Button) view);
            String guess = guessButton.getText().toString();
            String answer = getCountryName(correctAnswer);
            ++totalGuesses;//increment number of guesses the user has made


            if (guess.equals(answer)) {// if the guess is correct
                ++correctAnswers;//incremnet the nuber of correct answers

                //display correct answer in green text

                answerTextView.setText(answer + "!");
                answerTextView.setTextColor(getResources().getColor(R.color.correct_answer, getContext().getTheme()));

                disableButtons();//disable all guess buttons

                // if the user has correctly identified FLAGS_IN_QUIZ flags
                if (correctAnswers == FLAG_IN_QUIZ) {
                    //dailogFragment to display quiz stats and start new quiz

                    DialogFragment quizResults = new DialogFragment() {
                        //create an AlertDialog and return it


                        @Override
                        public Dialog onCreateDialog(Bundle bundle) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                            builder.setMessage(getString(R.string.results, totalGuesses, (1000 / (double) totalGuesses)));

                            //reset quiz button
                            builder.setPositiveButton("Reset Quiz", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            resetQuiz();
                                        }
                                    }
                            );
                            return builder.create(); // return the AlertDialog
                        }
                    };

                    //use Fragment manager to display the DialogFragment

                    quizResults.setCancelable(false);
                    quizResults.show(getFragmentManager(), "quiz results");
                } else {//answer is correct but quiz is not over
                    //load the next flag after a 2-second delay
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            animate(true);
                        }
                    }, 2000); // 200 millisecons for 2-second delay


                }
            } else {//answer  is incorrect
                flagImageView.startAnimation(shakeAnimation);//play shake

                //display "incorrect!" in red

                answerTextView.setTextColor(getResources().getColor(R.color.incorrect_answer, getContext().getTheme()));
                ;
                guessButton.setEnabled(false);//disable incorretanswer

            }
        }
    };

    private  void disableButtons(){

        for (int row =0;row<guessRows;row++){
            LinearLayout guessRow = guessLinearLayouts[row];
            for(int i=0;i<guessRow.getChildCount();i++){
                guessRow.getChildAt(i).setEnabled(false);
            }
        }
    }

}

