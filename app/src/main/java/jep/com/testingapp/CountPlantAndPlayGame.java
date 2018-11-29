package jep.com.testingapp;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class CountPlantAndPlayGame extends Fragment {

    private Button playButton;
    private Button stopButton;
    private ArrayList<ArrayList<String>> listOfPlants5km=new ArrayList<>();
    private ArrayList<String> plant5km=new ArrayList<>();
    private Object choosenPlant=new ArrayList<>();
    sendChoosenPlant mCallback;
    sendStopGame mCallback1;

    public interface sendChoosenPlant {
        public void onPlantSelected(ArrayList<String> position);
    }
    public interface sendStopGame {
        public void onStopGame(boolean stop);
    }


    public void setOnPlantSelectedListener(sendChoosenPlant activity) {
        mCallback = activity;
    }
    public void setOnStopGameListener(sendStopGame activity) {
        mCallback1 = activity;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.countandgame_fragment, container, false);
        stopButton = view.findViewById(R.id.buttonStop);
        stopButton.setVisibility(View.INVISIBLE);
        playButton = view.findViewById(R.id.buttonPlay);
        playButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {

                PlayGame(listOfPlants5km);
                listOfPlants5km=new ArrayList<>();


            }
        });
        stopButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {

                mCallback1.onStopGame(false);


            }
        });

        TextView numberPlant = view.findViewById(R.id.numberPlant);
        numberPlant.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (getArguments()!=null) {
                    plant5km = CountPlantAndPlayGame.this.getArguments().getStringArrayList("listPlants");
                    if (!listOfPlants5km.contains(plant5km)){
                        listOfPlants5km.add(plant5km);

                        Log.i("mytag2", plant5km.toString());
                    }

                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });



        return view;
    }



    private void PlayGame(final ArrayList<ArrayList<String>> PlantsAt5km){
        ArrayList<String> nameOfPlants=new ArrayList<>();

        if (!PlantsAt5km.isEmpty()) {
            for (ArrayList<String> plant:PlantsAt5km
                    ) {
                nameOfPlants.add(plant.get(0));
            }
            LayoutInflater allPlantDisplay = LayoutInflater.from(getContext());
            final View alertDialogView = allPlantDisplay.inflate(R.layout.dialog_start_game, null);
            //create alertDialog
            final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            //set our view for alertDialog
            builder.setView(alertDialogView);
            final AlertDialog alertDialog=builder.create();
            alertDialog.setTitle("Choose your plant to start the game");
            alertDialog.show();

            ListView mListView = alertDialogView.findViewById(R.id.plantForGame);
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(getContext(),
                    android.R.layout.simple_list_item_1, nameOfPlants);
            mListView.setAdapter(adapter);
            mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position,
                                        long id) {
                    choosenPlant=parent.getItemAtPosition(position);
                    choosenPlant.toString();
                    for (ArrayList<String> plant:PlantsAt5km
                            ) {

                        if (plant.contains(choosenPlant.toString())){

                            mCallback.onPlantSelected(plant);
                            alertDialog.cancel();
                            if (plant.size()>1){
                                AlertDialog.Builder builderChoosen = new AlertDialog.Builder(getContext());
                                AlertDialog alertDialogChoosen=builderChoosen.create();
                                alertDialogChoosen.setTitle("You have choosen this plant: "+plant.get(0));
                                alertDialogChoosen.show();

                            }

                        }
                    }


                }
            });



        }


    }




    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);



    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

    }

    @Override
    public void onPause() {
        super.onPause();
    }


}
