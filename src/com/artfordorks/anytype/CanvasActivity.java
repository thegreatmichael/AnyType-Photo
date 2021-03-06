/**************************************************************                                                                       
AnyType                                    
Copyright (C) 2012-2013 by Laura Devendorf     
www.ischool.berkeley.edu/~ldevendorf/anytype                  
---------------------------------------------------------------             
                                                                           
This file is part of AnyType.

AnyType is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

AnyType is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with AnyTypePhoto. If not, see <http://www.gnu.org/licenses/>.

*****************************************************************/
package com.artfordorks.anytype;

/***
 * This class draws a shape to the screen and allows the user 
 * to capture that shape
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.artfordorks.anytype.R.id;



public class CanvasActivity extends Activity{

	private LetterView letter_view;
	private boolean two_finger = false;
	private int savedNum = 0;
	private GestureDetector gd;
	private ScaleGestureDetector sd;
	private GridView canvas_letter_grid;
	private boolean sequential = false;
	private boolean reverse_order = false;
	private View view;
	private boolean recording = false;
	private int rec_count = 0;

	private double beginTime = System.currentTimeMillis();


	
	private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
	    @Override
	    public boolean onScale(ScaleGestureDetector detector) {
	    	double scale = Globals.getScale(detector.getCurrentSpan());
	    	letter_view.updateScale(scale);
			letter_view.updatePosition(detector.getFocusX(), detector.getFocusY());
	        letter_view.invalidate();
	        return true;
	    }
	}
	
	private class GestureListener extends SimpleOnGestureListener {
		private static final int SWIPE_MIN_DISTANCE = 120;
	    private static final int SWIPE_MAX_OFF_PATH = 250;
	    private static final int SWIPE_THRESHOLD_VELOCITY = 1000;
	    
	    
	    public void onShowPress(MotionEvent e){
	    	//set current id
	    	int selected = letter_view.locate((int) e.getX(), (int) e.getY());
			if (selected != letter_view.getCur() && letter_view.getCur() != -1)
				letter_view.deselect(letter_view.getCur());
			if(selected != -1) letter_view.select(selected);
			
	    }
		
	    public void onLongPress(MotionEvent e){
	    	int selected = letter_view.locate((int) e.getX(), (int) e.getY());			
	    	if (selected != -1){
	    		
	    		letter_view.stopAllVideos();
	    		
				launchLetterPartView(letter_view.getCurLetterId());
	    		//launchLetterEditor(letter_view.getCurLetterId());
	    		
	    		letter_view.deselect(selected);
			}
	    }
	    
	    public boolean onDoubleTap(MotionEvent e){
	    	int selected = letter_view.locate((int) e.getX(), (int) e.getY());			
	    	if (selected != -1){
				//launchLetterPartView(letter_view.getSelectedLetterId(selected));
	    		
	    		//launchLetterEditor(letter_view.getCurLetterId());
	    		
	    		if(Globals.using_video){
	    			
	    			 boolean same_letter = letter_view.isPlaying(selected);
	    			
	    			if(same_letter){
	    				letter_view.stopLetterVideo(selected);
	    			}else{
	    				//if a new letter is selected for playing, stop the original video first
	    				Log.d("Async", "Selected Play Video "+selected);
	    				letter_view.playLetterVideo(selected); 
	    			}
	    		}
	    		
	    		//letter_view.deselect(selected);	    		
	    		return true;

			}
    		return false;

	    }
	    	    
	    @Override
	    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
	        try {
	            if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH)
	                return false;
	            if(e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY || 
	            		(e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY)) {
	                

	            	
	            	letter_view.removeCurrentLetter(); //this handles stopping the video
	                
	        		LinearLayout ll = (LinearLayout) findViewById(R.id.instructions);
	        		
	        		if(!letter_view.hasLetters()) ll.setVisibility(View.VISIBLE);
	                
	    	    	letter_view.invalidate();

	                //Toast.makeText(CanvasActivity.this, "Deleted", Toast.LENGTH_SHORT).show();

	            }
	        } catch (Exception e) {
	            // nothing
	        }
	        return false;
	    }
	    
	}



	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.canvas);
		
		view = (View) findViewById(R.id.top_view);
		
		gd = new GestureDetector(CanvasActivity.this, new GestureListener(), new Handler());
		sd = new ScaleGestureDetector(CanvasActivity.this, new ScaleListener());
		
		canvas_letter_grid = (GridView) findViewById(R.id.letter_grid);
		canvas_letter_grid.setAdapter(new LetterAdapter(this));

		

		
		
		if(Globals.using_video) letter_view = new LetterViewDynamic(this, sequential);
		else letter_view = new LetterViewStatic(this);
		
		
		letter_view.setClickable(true);
		FrameLayout canvas = (FrameLayout) findViewById(R.id.canvas_frame);
		canvas.addView(letter_view, new LayoutParams(LayoutParams.WRAP_CONTENT,
				LayoutParams.WRAP_CONTENT));
		
		letter_view.setOnTouchListener(new OnTouchListener() {
	        @Override
	        public boolean onTouch(final View view, final MotionEvent event) {

				
				boolean action = gd.onTouchEvent(event);
				boolean scaled = sd.onTouchEvent(event);
				
				if(!action ){
            	LetterView lv = (LetterView) view;
    	    	
            	int selected = lv.getCur();
            	if(selected == -1) selected = lv.locate((int) event.getX(), (int) event.getY());
    	    	if(selected == -1) return true;
    	    	lv.select(selected);
    	    	
    	    	Log.d("Tap", "Selected: " + selected);

    	    	
    	    	//lv.select(selected);
          
	            if (event.getActionIndex() > 0) {
	    			two_finger = !two_finger;
	            }
	            
	            //check for move and stretches
	            if (event.getAction() == MotionEvent.ACTION_MOVE) {
	                    	
	    		
	    			if(event.findPointerIndex(0) != -1 && event.findPointerIndex(1) != -1){
	    				float r = (float)Globals.getRotation(event.getX(0), event.getY(0), event.getX(1), event.getY(1));
	    				lv.incRotations(r);
	    				
	    				//set positions inbetween the two points
	    				lv.updatePosition((event.getX(0)+event.getX(1))/2, (event.getY(0)+event.getY(1))/2);
	    				
	    			}else{
	    				lv.updatePosition(event.getX(), event.getY());
	    			}
	    
	            }else if(event.getAction() == MotionEvent.ACTION_UP){
	            	lv.deselect(selected);
	            	Globals.clearFingerData();
	            }
	            lv.invalidate();
				}
	            return true;
	        }
	     });
		
		
		if(Globals.saved_lv != null){
			letter_view.loadState(Globals.saved_lv);
			Globals.saved_lv = null;	
		}

		ToggleButton recordButton = (ToggleButton) findViewById(id.button_record);
		recordButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				recording = !recording;
				rec_count = 0;
			}
		});

		Button saveButton = (Button) findViewById(id.button_save_canvas);
		saveButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				saveCanvasDialog();
			}
		});
		
		Button emailButton = (Button) findViewById(id.button_email);
		emailButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String s = "MyCanvas_" +(savedNum++)+ Globals.timeStamp;
				File f = saveScreen(s);
				sendEmail(f);
			}
		});
		
		Button resetButton = (Button) findViewById(id.button_reset);
		resetButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				reset();
			}
		});
		
		Button saveFontButton = (Button) findViewById(id.button_save_font);
		saveFontButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				saveFontDialog();
			}
		});
		
		
		ToggleButton sequentialButton = (ToggleButton) findViewById(id.button_sequence);
		sequentialButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				toggleSequential();
			}
		});
		
		
		
		ToggleButton reverseButton = (ToggleButton) findViewById(id.button_reverse);
		reverseButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				reverse_order = !reverse_order;
				toggleReverseOrder();
			}
		});
		
		TextView label_reverse = (TextView) findViewById(id.text_reverse);
		TextView label_sequence = (TextView) findViewById(id.text_sequence);
		
		if(!Globals.using_video){
			sequentialButton.setVisibility(View.INVISIBLE);
			reverseButton.setVisibility(View.INVISIBLE);
			label_reverse.setVisibility(View.INVISIBLE);
			label_sequence.setVisibility(View.INVISIBLE);
			recordButton.setVisibility(View.INVISIBLE);
		}
			
		
		LinearLayout ll = (LinearLayout) findViewById(R.id.instructions);
		if(letter_view.hasLetters()) ll.setVisibility(View.INVISIBLE);
	}
	
	public void toggleSequential(){
		sequential = !sequential;
		((LetterViewDynamic)letter_view).toggleSequential(sequential);
		
	}
	
	public void toggleReverseOrder(){
		letter_view.setReverse(reverse_order);		
	}
	

	
	public void writeToLog(String to){
		double endTime = System.currentTimeMillis();
		double time = endTime - beginTime;
		Globals.writeToLog(this, getLocalClassName(), to, time);
	}
	
	
	public void sendEmail(File file){
		Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);  
		  
		String aEmailBCCList[] = { ""};  
		emailIntent.setType("plain/text");  
		emailIntent.putExtra(android.content.Intent.EXTRA_BCC, aEmailBCCList);  
		emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "A Typeface For You");  
		emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, "See image attached.");  

		
		if (!file.exists() || !file.canRead()) {
		    Toast.makeText(this, "Attachment Error", Toast.LENGTH_SHORT).show();
		    finish();
		    return;
		}
		
		Uri uri = Uri.parse("file://" + file);
		emailIntent.putExtra(Intent.EXTRA_STREAM, uri);
		
		double endTime = System.currentTimeMillis();
		double time = endTime - beginTime;
		Globals.writeToLog(this, this.getLocalClassName(), "_Email", time);

		startActivity(emailIntent);
	}
	
	public void saveFontDialog(){
		double endTime = System.currentTimeMillis();
		double time = endTime - beginTime;
		Globals.writeToLog(this, this.getLocalClassName(), "_SaveFont", time);
		
		AlertDialog.Builder alert = new AlertDialog.Builder(this);

		alert.setTitle("Save As");
		alert.setMessage("Enter a Name for this Typeface");

		// Set an EditText view to get user input 
		final EditText input = new EditText(this);
		alert.setView(input);

		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int whichButton) {
		  String value = input.getText().toString();
		  	boolean success = Globals.renameDirectory(value);
		  	Log.d("Delete", "Rename: "+success);
		  }
		});

		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
		  public void onClick(DialogInterface dialog, int whichButton) {
		    // Canceled.
		  }
		});

		alert.show();
	}
	
	public void saveCanvasDialog(){
		double endTime = System.currentTimeMillis();
		double time = endTime - beginTime;
		Globals.writeToLog(this, this.getLocalClassName(), "_SaveCanvas", time);
		
		AlertDialog.Builder alert = new AlertDialog.Builder(this);

		alert.setTitle("Save Canvas As");
		alert.setMessage("Enter a Name for this Canvas");
		
		

		// Set an EditText view to get user input 
		final EditText input = new EditText(this);
		alert.setView(input);

		
		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int whichButton) {
		  String value = input.getText().toString();
		  saveScreen(value);
		  }
		});

		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
		  public void onClick(DialogInterface dialog, int whichButton) {
		    // Canceled.
		  }
		});

		alert.show();
	}
	
	@Override
	protected void onPause() {
		saveCanvasState();
		super.onPause();
	}

	void saveCanvasState(){
		Globals.saved_lv = letter_view;
	}
	
	private File saveScreen(String name){
		
		double endTime = System.currentTimeMillis();
		double time = endTime - beginTime;
		Globals.writeToLog(this, this.getLocalClassName(), "_SaveScreen", time);
		//save the picture
		

		File pictureFile = Globals.getOutputUrbanProto(Globals.MEDIA_TYPE_IMAGE, name + ".png");
		
		if (pictureFile == null) {
			return null;
		}


		try {
			FileOutputStream fos = new FileOutputStream(pictureFile);
			Bitmap bmap = letter_view.getImageOut();
			bmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
			fos.close();
			Log.d("Capture Activity", "File Created");
			

		} catch (FileNotFoundException e) {
			Log.d("", "File not found: " + e.getMessage());
		} catch (IOException e) {
			Log.d("", "Error accessing file: " + e.getMessage());
		}
		
		
	    Toast.makeText(this, "Canvas Saved As "+name+"!", Toast.LENGTH_SHORT).show();

		return pictureFile;
		
	}
	
	public void reset(){
		
		double endTime = System.currentTimeMillis();
		double time = endTime - beginTime;
		Globals.writeToLog(this, this.getLocalClassName(), "LoadActivity", time);
		
		
		Intent intent = new Intent(this, LoadActivity.class);
		startActivity(intent);
	}
	
	public void addToCanvas(ImageView v, MotionEvent e) {
		float x = (letter_view.getWidth()/26*v.getId())+e.getX();
		double endTime = System.currentTimeMillis();
		double time = endTime - beginTime;
		Globals.writeToLog(this, this.getLocalClassName(), "_AddLetter - "+Globals.intToChar(v.getId()), time);
		
		letter_view.addLetter(v.getId());
		letter_view.invalidate();
		letter_view.updatePosition(x, 61f);	
		
		LinearLayout ll = (LinearLayout) findViewById(R.id.instructions);
		ll.setVisibility(View.INVISIBLE);

	}
	
	public void launchLetterPartView(int letter_id){
		Globals.force_letter = letter_id;
		
		saveCanvasState();
		
		double endTime = System.currentTimeMillis();
		double time = endTime - beginTime;
		Globals.writeToLog(this, this.getLocalClassName(), "LetterVideoPlayerActivity", time);
		
		
		Intent intent = new Intent(this, LetterPartViewerActivity.class);
		startActivity(intent);
	}
	
	public void launchLetterEditor(int letter_id){
		Globals.force_letter = letter_id;
		
		saveCanvasState();
		
		double endTime = System.currentTimeMillis();
		double time = endTime - beginTime;
		Globals.writeToLog(this, this.getLocalClassName(), "LetterVideoPlayerActivity", time);
		
		
		Intent intent = new Intent(this, LetterEditActivity.class);
		startActivity(intent);
	}
	
	//if record is on do this every time the screen frame changes
	public void saveScreen(){
		View v = view.getRootView();
        v.setDrawingCacheEnabled(true);
        Bitmap b = v.getDrawingCache();             
        String extr = Globals.getTestPath();
       
        File myPath = new File(extr, "SS_"+System.currentTimeMillis()+"_"+rec_count+"_"+rec_count/Globals.frames_per_second+".jpg");
        Log.d("Path", "Saved to "+myPath.getAbsolutePath());
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(myPath);
            b.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
            MediaStore.Images.Media.insertImage(getContentResolver(), b, "Screen", "screen");
        }catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        rec_count++;
	}
	
	public boolean isRecording(){
		return recording;
	}
	

	
	

}


