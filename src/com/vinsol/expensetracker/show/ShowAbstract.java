/**
 * Copyright (c) 2012 Vinayak Solutions Private Limited 
 * See the file license.txt for copying permission.
*/     


package com.vinsol.expensetracker.show;

import java.io.File;
import java.util.Calendar;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnDismissListener;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.flurry.android.FlurryAgent;
import com.vinsol.expensetracker.DatabaseAdapter;
import com.vinsol.expensetracker.R;
import com.vinsol.expensetracker.helpers.CheckEntryComplete;
import com.vinsol.expensetracker.helpers.DeleteDialog;
import com.vinsol.expensetracker.helpers.FileHelper;
import com.vinsol.expensetracker.helpers.SharedPreferencesHelper;
import com.vinsol.expensetracker.listing.ExpenseListing;
import com.vinsol.expensetracker.listing.ExpenseSubListing;
import com.vinsol.expensetracker.models.Entry;

abstract class ShowAbstract extends Activity implements OnClickListener {

	protected TextView showAmount;
	protected TextView showTag;
	protected Entry mShowList;
	protected Bundle intentExtras;
	protected int typeOfEntryFinished;
	protected int typeOfEntryUnfinished;
	protected int typeOfEntry;
	protected TextView showHeaderTitle;
	protected final int SHOW_RESULT = 35;
	protected DatabaseAdapter mDatabaseAdapter;	
	protected Button showDelete;
	protected Button showEdit;
	private ToggleButton showAddFavorite;
	private TextView showAddFavoriteTextView;
	private String tempfavID;
	protected FileHelper fileHelper;
	
	@Override
	protected void onStart() {
		super.onStart();
		FlurryAgent.onStartSession(this, getString(R.string.flurry_key));
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		FlurryAgent.onEndSession(this);
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.show_page);
		fileHelper = new FileHelper();
		showEdit = (Button) findViewById(R.id.show_edit);
		showAddFavorite = (ToggleButton) findViewById(R.id.show_add_favorite);
		showAddFavoriteTextView = (TextView) findViewById(R.id.show_add_favorite_textView);
		showDelete = (Button) findViewById(R.id.show_delete);
		showHeaderTitle = (TextView) findViewById(R.id.header_title);
		mDatabaseAdapter = new DatabaseAdapter(this);
		showAmount = (TextView) findViewById(R.id.show_amount);
		showTag = (TextView) findViewById(R.id.show_tag_textview);
		showEdit.setOnClickListener(this);
		showDelete.setOnClickListener(this);
	}
	
	public void showHelper() {
		// ///// ****** Assigning memory ******* /////////
		
		if (intentExtras.containsKey("mDisplayList")) {
			mShowList = intentExtras.getParcelable("mDisplayList");

			if (!(mShowList.amount.equals("") || mShowList.amount == null)) {
				if (!mShowList.amount.contains("?"))
					showAmount.setText(mShowList.amount);
			}
			
			if (!(mShowList.description.equals("") || mShowList.description == null || mShowList.description.equals(getString(typeOfEntryUnfinished)))) {
				showTag.setText(mShowList.description);
			} else {
				showTag.setText(getString(typeOfEntryFinished));
			}
			
			Calendar mCalendar = Calendar.getInstance();
			mCalendar.setTimeInMillis(mShowList.timeInMillis);
			mCalendar.setFirstDayOfWeek(Calendar.MONDAY);
			
			if(mShowList.location != null)
				new ShowLocationHandler(this, mShowList.location);

			if(mShowList.timeInMillis != null) {
				new ShowDateHandler(this, mShowList.timeInMillis);
			}
			else {
				new ShowDateHandler(this,typeOfEntry);
			}
			
			tempfavID = mShowList.favId;
		}
	}
	
	public void doTaskOnActivityResult() {
		if (intentExtras.containsKey("mDisplayList")) {
			mShowList = intentExtras.getParcelable("mDisplayList");
			
			if(!new CheckEntryComplete().isEntryComplete(mShowList, this)) {
				finish();
			}
			
			if (!(mShowList.amount.equals("") || mShowList.amount == null)) {
				if (!mShowList.amount.contains("?"))
					if(mShowList.amount.endsWith(".0")) {
						mShowList.amount = mShowList.amount+"0";
						showAmount.setText(mShowList.amount);
					}
					else { 
						showAmount.setText(mShowList.amount);
					}
				else
					showAmount.setText("?");
			} else {
				showAmount.setText("?");
			}
			
			if (!(mShowList.description.equals("") || mShowList.description == null || mShowList.description.equals(getString(typeOfEntryUnfinished)) || mShowList.description.equals(getString(typeOfEntryFinished)))) {
				showTag.setText(mShowList.description);
			} else {
				showTag.setText(getString(typeOfEntryFinished));
			}
			
			Calendar mCalendar = Calendar.getInstance();
			mCalendar.setTimeInMillis(mShowList.timeInMillis);
			mCalendar.setFirstDayOfWeek(Calendar.MONDAY);
			
			if(mShowList.location != null)
				new ShowLocationHandler(this, mShowList.location);
			
			if(mShowList.timeInMillis != null)
				new ShowDateHandler(this, mShowList.timeInMillis);
			else {
				new ShowDateHandler(this,typeOfEntry);
			}
		}
		setResultModifiedToListing();
	}
	
	@Override
	public void onClick(View v) {
		
		switch (v.getId()) {
		
			case R.id.show_delete:
				if(new SharedPreferencesHelper(this).getSharedPreferences().getBoolean(getString(R.string.pref_key_delete_dialog), false)) {
					showDeleteDialog();
				} else {
					delete();
				}
				break;
	
			case R.id.show_edit:
				FlurryAgent.onEvent(getString(R.string.edit_button));
				intentExtras.putBoolean("isFromShowPage", true);
				intentExtras.remove("mDisplayList");
				if(mShowList.amount.endsWith(".00")) {
					mShowList.amount = mShowList.amount.substring(0, mShowList.amount.length()-3);
				}
				intentExtras.putParcelable("mDisplayList", mShowList);
				editAction();
				break;
				
			case R.id.show_add_favorite:
			case R.id.show_add_favorite_textView:
				FlurryAgent.onEvent(getString(R.string.added_to_favorite));
				Boolean toCheck;
				if(v.getId() == R.id.show_add_favorite) {
					toCheck = showAddFavorite.isChecked();
				} else {
					toCheck = !showAddFavorite.isChecked();
				}
				onClickFavorite(toCheck);
				break;
				
			default:
				break;
		}
	}

	private void delete() {
		FlurryAgent.onEvent(getString(R.string.delete_button));
		if (mShowList.id != null) {
			deleteFile();
			mDatabaseAdapter.open();
			mDatabaseAdapter.deleteEntryTableEntryID(mShowList.id);
			mDatabaseAdapter.close();
			Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show();
			if(intentExtras.containsKey("position")) {
				intentExtras.putBoolean("isChanged", true);
				setResultModifiedToListing();
			}
			finish();
		} else {
			Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();
		}
	}
	
	private void showDeleteDialog() {
		final DeleteDialog mDeleteDialog = new DeleteDialog(this);
		mDeleteDialog.show();
		mDeleteDialog.setOnDismissListener(new OnDismissListener() {
			
			@Override
			public void onDismiss(DialogInterface dialog) {
				if(mDeleteDialog.isDelete()) {
					delete();
				}
			}
		});
	}

	private void setResultModifiedToListing() {
		if(!istempfavIdequalsfavId()) {
			intentExtras.putBoolean("isChanged", true);
		}
		Intent intent = new Intent(this, ExpenseListing.class);
		intent.putExtras(intentExtras);
		setResult(Activity.RESULT_OK, intent);
		intent = new Intent(this, ExpenseSubListing.class);
		intent.putExtras(intentExtras);
		setResult(Activity.RESULT_OK, intent);
	}

	private boolean istempfavIdequalsfavId() {
		if(tempfavID == null && mShowList.favId == null) {
			return true;
		}
		if(tempfavID == null || mShowList.favId == null) {
			return false;
		}
		if(tempfavID.equals(mShowList.favId)) {
			return true;
		}
		return false;
	}
	
	public void FavoriteHelper() {
		showAddFavorite.setVisibility(View.VISIBLE);
		showAddFavoriteTextView.setVisibility(View.VISIBLE);
		if(mShowList.favId != null) {
			if(!mShowList.favId.equals("")) {
				showAddFavorite.setChecked(true);
				showAddFavoriteTextView.setText("Remove from Favorite");
			} else {
				showAddFavoriteTextView.setText("Add to Favorite");
				showAddFavorite.setChecked(false);
			}
		} else {
			showAddFavoriteTextView.setText("Add to Favorite");
			showAddFavorite.setChecked(false);
		}
		showAddFavorite.setOnClickListener(this);
		showAddFavoriteTextView.setOnClickListener(this);
	}

	public void onClickFavorite(Boolean toCheck) {
		Long favID = null;
		if(toCheck) {
			if(mShowList.type.equals(getString(R.string.text))) {
				mDatabaseAdapter.open();
				favID = mDatabaseAdapter.insertToFavoriteTable(mShowList);
				mDatabaseAdapter.close();
			} else if(mShowList.type.equals(getString(R.string.camera))) {
				if (android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {
					try {
						mDatabaseAdapter.open();
						favID = mDatabaseAdapter.insertToFavoriteTable(mShowList);
						mDatabaseAdapter.close();
						
						fileHelper.copyAllToFavorite(mShowList.id, favID.toString());
						File mFile = fileHelper.getCameraFileLargeFavorite(favID.toString());
						File mFileSmall = fileHelper.getCameraFileSmallFavorite(favID.toString());
						File mFileThumbnail = fileHelper.getCameraFileThumbnailFavorite(favID.toString());
						if(mFile.canRead() && mFileSmall.canRead() && mFileThumbnail.canRead()) {
						} else {
							mDatabaseAdapter.open();
							mDatabaseAdapter.deleteFavoriteTableEntryID(favID);
							mDatabaseAdapter.close();
						}
					} catch (Exception e) {	
					}
				} else {
					Toast.makeText(this, "sdcard not available", Toast.LENGTH_SHORT).show();
				}
			} else if(mShowList.type.equals(getString(R.string.voice))) {
				if (android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {
					try {
						mDatabaseAdapter.open();
						favID = mDatabaseAdapter.insertToFavoriteTable(mShowList);
						mDatabaseAdapter.close();
						fileHelper.copyAllToFavorite(mShowList.id, favID.toString());
						File mFile = fileHelper.getAudioFileFavorite(favID.toString());
						if(!mFile.canRead()) {
							mDatabaseAdapter.open();
							mDatabaseAdapter.deleteFavoriteTableEntryID(favID);
							mDatabaseAdapter.close();
						}
					} catch (Exception e) {	
					}
				} else {
					Toast.makeText(this, "sdcard not available", Toast.LENGTH_SHORT).show();
				}
			}
			mShowList.favId = favID+"";
			mDatabaseAdapter.open();
			mDatabaseAdapter.editEntryTable(mShowList);
			mDatabaseAdapter.close();
			showAddFavorite.setChecked(true);
			showAddFavoriteTextView.setText("Remove from Favorite");
		} else if(mShowList.id.equals(getString(R.string.text))) {
				mDatabaseAdapter.open();
				favID = mDatabaseAdapter.getFavoriteIdEntryTable(mShowList.id);
				mDatabaseAdapter.close();
				doTaskAfter(favID);
			} else if (android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {
				mDatabaseAdapter.open();
				favID = mDatabaseAdapter.getFavoriteIdEntryTable(mShowList.id);
				mDatabaseAdapter.close();
				fileHelper.deleteAllFavoriteFiles(favID.toString());
				doTaskAfter(favID);
			} else {
				Toast.makeText(this, "sdcard not available", Toast.LENGTH_SHORT).show();
			}
	}

	private void doTaskAfter(Long favID) {
		mDatabaseAdapter.open();
		mDatabaseAdapter.deleteFavoriteTableEntryID(favID);
		mDatabaseAdapter.close();
		mDatabaseAdapter.open();
		mDatabaseAdapter.editFavoriteIdEntryTable(favID);
		mDatabaseAdapter.close();
		showAddFavorite.setChecked(false);
		mShowList.favId = null;
		showAddFavoriteTextView.setText("Add to Favorite");
	}	

	@Override
	public void onBackPressed() {
		FlurryAgent.onEvent(getString(R.string.back_pressed));
		setResultModifiedToListing();
		finish();
		super.onBackPressed();
	}
	
	protected abstract void editAction();

	protected void deleteFile() {}

}
