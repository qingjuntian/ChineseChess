package com.thor.chess;

import andren.game.china.chess.R;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Spinner;

public class AIGameDialog extends Dialog {
	class SpinnerSelectedListener implements OnItemSelectedListener{  

	    public void onItemSelected(AdapterView<?> adapterView, View view, int position,  
	            long arg3) {  
	    	((ChessMain)listener).mSelectedItem = position;
	    	ChessApplication.setSetting("DifficultyLevel", String.valueOf(position));
	    }  

	    public void onNothingSelected(AdapterView<?> arg0) {  
	    }  
	}  
	
	private GameListener listener = null;	
	public AIGameDialog(Context context) {
		super(context);
	}
	
	public void setListener(GameListener listener) {
		this.listener = listener;
	}	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.ai_menu);
		setTitle(R.string.ai_game);
		initDifficultLevel();
		
		Button btnStart = (Button)findViewById(R.id.btn_start_game);
		btnStart.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				int player = 0;
				float searchSeconds = 0.5f;
				RadioGroup radioColor = 
						(RadioGroup)findViewById(R.id.rad_player_color);
				if (radioColor.getCheckedRadioButtonId() == R.id.radio_red) {
					player = 0;
				} else {
					player = 1;
				}
				
//				RadioGroup radioLevel = 
//						(RadioGroup)findViewById(R.id.radio_level);
//				if (radioLevel.getCheckedRadioButtonId() == R.id.radio_easy) {
//					searchSeconds = 0.5f;
//				} else if (radioLevel.getCheckedRadioButtonId() == R.id.radio_normal) {
//					searchSeconds = 1.5f;
//				} else {
//					searchSeconds = 2.5f;
//				}
				
				AIEngine engine = new AIEngine(player, searchSeconds);
				
				dismiss();
				if (listener != null)
					listener.onCreateEngine(engine, false);
			}
		});
	}
	
	private void initDifficultLevel() {
		Spinner spinner = (Spinner) findViewById(R.id.select_level); 
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
				getContext(), R.array.levels, android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); 
		spinner.setAdapter(adapter); 
		spinner.setOnItemSelectedListener(new SpinnerSelectedListener());  
		spinner.setSelection(((ChessMain)listener).mSelectedItem);
		spinner.setVisibility(View.VISIBLE); 
	}
}

//class AIGameDialog extends DialogFragment {
//	private AIEngine engine = null;
//	private GameListener listener = null;
//	public AIEngine getEngine() {
//		return engine;
//	}
//	
//	public void setListener(GameListener listener) {
//		this.listener = listener;
//	}
//
//	@Override
//	public Dialog onCreateDialog(Bundle savedInstanceState) {
//		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
//		LayoutInflater inflater = getActivity().getLayoutInflater();
//		final View view = inflater.inflate(R.layout.ai_menu, null);
//		builder.setTitle(R.string.ai_game);
//		builder.setView(view).setPositiveButton(R.string.start_game,
//				new DialogInterface.OnClickListener() {
//					public void onClick(DialogInterface dialog, int which) {
//						int player = 0;
//						float searchSeconds = 0.5f;
//						RadioGroup radioColor = (RadioGroup) view
//								.findViewById(R.id.rad_player_color);
//						if (radioColor.getCheckedRadioButtonId() == R.id.radio_red) {
//							player = 0;
//						} else {
//							player = 1;
//						}
//						RadioGroup radioLevel = (RadioGroup) view
//								.findViewById(R.id.radio_level);
//						if (radioLevel.getCheckedRadioButtonId() == R.id.radio_easy) {
//							searchSeconds = 0.5f;
//						} else if (radioLevel.getCheckedRadioButtonId() == R.id.radio_easy) {
//							searchSeconds = 2f;
//						} else {
//							searchSeconds = 3f;
//						}
//						engine = new AIEngine(player, searchSeconds);
//						listener.onCreateEngine(engine);
//					}
//				});
//		return builder.create();
//	}
//}