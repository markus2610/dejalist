package com.luboganev.dejalist.data;

import static nl.qbusict.cupboard.CupboardFactory.cupboard;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import nl.qbusict.cupboard.QueryResultIterable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.luboganev.dejalist.R;
import com.luboganev.dejalist.data.DejalistContract.Categories;
import com.luboganev.dejalist.data.DejalistContract.Products;
import com.luboganev.dejalist.data.entities.Category;
import com.luboganev.dejalist.data.entities.Product;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.app.NotificationCompat;

public class BackupIntentService extends IntentService {
	private static final String SERVICE_NAME = "BackupRestoreService";
	public static final String INTENT_EXTRA_ACTION = "action";
	private static final int NOTIFICATION_ID = 42; // The answer to everything
	public static enum Action {BACKUP, RESTORE}; 
	
	private static final String BACKUP_FOLDER_NAME = "DejalistBackup";
	private static final String BACKUP_FOLDER_NOMEDIA = ".nomedia";
	private static final String BACKUP_METADATA_FILE_NAME = "data.txt";
	
	private static final String CATEGORY_BACKUP_PRODUCTS_JSON = "products";
	private static final String PRODUCT_IMAGE_FILENAME = "image_filename";
	
	private JSONArray mData;
	private List<File> mProductImageFiles;
	private NotificationManager mNotifManager;
	private NotificationCompat.Builder mNotifBuilder;

	public BackupIntentService() {
		super(SERVICE_NAME);
		
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		mNotifManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
	}
	
	@Override
	protected void onHandleIntent(Intent intent) {
		Action receivedAction = (Action)intent.getSerializableExtra(INTENT_EXTRA_ACTION);
		mNotifBuilder = new NotificationCompat.Builder(getApplicationContext())
			.setSmallIcon(R.drawable.ic_stat_backup_restore);
		switch (receivedAction) {
		case BACKUP:
			mNotifBuilder.setContentTitle(getString(R.string.backup_notif_title))
				.setContentText(getString(R.string.backup_notif_text));
			mNotifBuilder.setProgress(0, 0, true);
			mNotifManager.notify(NOTIFICATION_ID, mNotifBuilder.build());
			if(backup()) mNotifBuilder.setContentText(getString(R.string.backup_notif_text_finished));
			else mNotifBuilder.setContentTitle(getString(R.string.backup_notif_title_failed))
					.setContentText("Errorstring");
			mNotifBuilder.setProgress(0,0,false);
            mNotifManager.notify(NOTIFICATION_ID, mNotifBuilder.build());
			break;
		case RESTORE:
			mNotifBuilder.setContentTitle(getString(R.string.restore_notif_title))
			.setContentText(getString(R.string.restore_notif_text));
			mNotifBuilder.setProgress(0, 0, true);
			mNotifManager.notify(NOTIFICATION_ID, mNotifBuilder.build());
			if(restore()) mNotifBuilder.setContentText(getString(R.string.restore_notif_text_finished));
			else mNotifBuilder.setContentTitle(getString(R.string.restore_notif_title_failed))
					.setContentText("Errorstring");
			mNotifBuilder.setProgress(0,0,false);
            mNotifManager.notify(NOTIFICATION_ID, mNotifBuilder.build());
			break;
		default:
			// do nothing
			break;
		}
	}
	
	private static File getBackupDir() {
		File backupDir = new File(Environment.getExternalStorageDirectory() + File.separator + BACKUP_FOLDER_NAME);
		if(!backupDir.exists()) backupDir.mkdir();
		return backupDir;
	}
	
	/* ------------------------------------- BACKUP ------------------------------------------ */
	
	private boolean clearBackupFolder() {
		File backupDir = getBackupDir();
		if(!backupDir.exists() || !backupDir.isDirectory()) {
			//TODO: show error
			return false;
		}
		for(File file: backupDir.listFiles()) file.delete();
		File nomedia = new File(backupDir, BACKUP_FOLDER_NOMEDIA);
		try {
			nomedia.createNewFile();
		} catch (IOException e) {
			//TODO: show error
			return false;
		}
		return true;
	}
	
	private static Category getNoCategory() {
		Category cat = new Category();
		cat._id = Products.PRODUCT_CATEGORY_NONE_ID;
		cat.name = "";
		cat.color = 0;
		return cat;
	}
	
	private static boolean backupCategoryWithProducts(Context context, Category category, JSONArray container, List<File> imageFilesContainer) {
		// backup the category
		JSONArray productsContainer = backupCategory(category, container);
		if(productsContainer == null) {
			return false;
		}
		// Get products with in this category
		QueryResultIterable<Product> productsItr = null;
		if(category._id == Products.PRODUCT_CATEGORY_NONE_ID) {
			productsItr = cupboard().withContext(context).query(Products.CONTENT_URI, Product.class).withSelection(Products.SELECTION_NO_CATEGORY, (String[])null).query();
		}
		else productsItr = cupboard().withContext(context).query(Products.buildCategoryProductsUri(category._id), Product.class).query();
		for (Product p : productsItr) {
			// backup each product in this category
			if(!backupProduct(p, productsContainer, imageFilesContainer)) {
				productsItr.close();
				return false;
			}
		}
		productsItr.close();
		return true;
	}
	
	private static JSONArray backupCategory(Category category, JSONArray container) {
		JSONObject obj = new JSONObject();
		JSONArray productsContainer = new JSONArray();
		try {
			obj.put(DejalistContract.Categories._ID, category._id);
			obj.put(DejalistContract.Categories.CATEGORY_NAME, category.name);
			obj.put(DejalistContract.Categories.CATEGORY_COLOR, category.color);
			
			// json array with all products that belong to this category
			obj.put(CATEGORY_BACKUP_PRODUCTS_JSON, productsContainer);
			
			// add to the json array with categories
			container.put(obj);
		} catch (JSONException e) {
			//TODO: show error
			return null;
		}
		return productsContainer;
	}
	
	private static boolean backupProduct(Product product, JSONArray container, List<File> imageFilesContainer) {
		JSONObject obj = new JSONObject();
		try {
			obj.put(DejalistContract.Products.PRODUCT_NAME, product.name);
			obj.put(DejalistContract.Products.PRODUCT_USED_COUNT, product.usedCount);
			obj.put(DejalistContract.Products.PRODUCT_LAST_USED, product.lastUsed);
			if(product.uri != null) {
				File imageFile = new File(Uri.parse(product.uri).getPath());
				obj.put(PRODUCT_IMAGE_FILENAME, imageFile.getName());
				imageFilesContainer.add(imageFile);
			}
			// add to the json array with products
			container.put(obj);
		} catch (JSONException e) {
			//TODO: show error
			return false;
		}
		return true;
	}
	
	public boolean writeBackup(File backupDir) {
		// validate backup dir
		if(!backupDir.exists() || !backupDir.isDirectory()) {
			//TODO: show error
			return false;
		}
		
		// backup the data file
		File productsFile = new File(backupDir, BACKUP_METADATA_FILE_NAME);
		FileOutputStream fos = null;
		try {
			productsFile.createNewFile();
			fos = new FileOutputStream(productsFile);
			fos.write(mData.toString().getBytes());
			fos.close();
		} catch (IOException e) {
			//TODO: show error
			if(fos != null) {
				try { fos.close(); } catch (IOException e1) {}
			}
			return false;
		}
		
		// backup the product images files
		for (File productImage : mProductImageFiles) {
			// backup the image file
			File backupProductFile = new File(backupDir, productImage.getName());
			try {
				backupProductFile.createNewFile();
			} catch (IOException e) {
				//TODO: show error
				return false;
			}
			if(!ProductImageFileHelper.copy(productImage, backupProductFile)) {
				//TODO: show error
				return false;
			}
		}
		return true;
	}
	
	private boolean backup() {
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// clear up and init
		clearBackupFolder();
		mData = new JSONArray();
		mProductImageFiles = new ArrayList<File>();
		
		// first add the products with no category to backup
		Category noCategory = getNoCategory();
		backupCategoryWithProducts(getApplicationContext(), noCategory, mData, mProductImageFiles);
		
		// now add all the real categories and their products
		QueryResultIterable<Category> categoriesItr = cupboard().withContext(getApplicationContext()).query(Categories.CONTENT_URI, Category.class).query();
		for (Category category : categoriesItr) {
			backupCategoryWithProducts(getApplicationContext(), category, mData, mProductImageFiles);
		}
		categoriesItr.close();
		
		// write the backup files
		return writeBackup(getBackupDir());
	}
	
	/* ------------------------------------- RESTORE ----------------------------------------- */
	
	private boolean restore() {
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		JSONArray backupData = readJsonArrayFile(new File(getBackupDir(), BACKUP_METADATA_FILE_NAME));
		if(backupData == null) return false;
		
		JSONObject categoryJson = null;
		for (int i = 0; i < backupData.length(); i++) {
			try { categoryJson = backupData.getJSONObject(i); } 
			catch (JSONException e) { 
				//TODO: show error
				return false;
			}
			if(!restoreCategoryWithProducts(getApplicationContext(), categoryJson)) return false;
		}
		
		return true;
	}
	
	private static boolean restoreProducts(Context context, JSONArray productsContainer, long categoryId) {
		for (int i = 0; i < productsContainer.length(); i++) {
			try { 
				JSONObject productJson = productsContainer.getJSONObject(i); 
				
				Product p = new Product();
				p.categoryId = categoryId;
				p.name = productJson.getString(DejalistContract.Products.PRODUCT_NAME);
				p.usedCount = productJson.getInt(DejalistContract.Products.PRODUCT_USED_COUNT);
				p.lastUsed = productJson.getLong(DejalistContract.Products.PRODUCT_LAST_USED);
				
				if(productJson.has(PRODUCT_IMAGE_FILENAME)) {
					String filename = productJson.getString(PRODUCT_IMAGE_FILENAME);
					File backupImageFile = new File(getBackupDir(), filename);
					if(backupImageFile.exists()) {
						File internalFile = ProductImageFileHelper.getFile(context, filename);
						try {
							int uniqueFileSuffix = 0;
							while(internalFile.exists()) {
								uniqueFileSuffix++;
								internalFile = ProductImageFileHelper.getFile(context, filename + "_" + uniqueFileSuffix);
							}
							internalFile.createNewFile();
						} catch (IOException e) {
							//TODO: show error
							return false;
						}
						ProductImageFileHelper.copy(backupImageFile, internalFile);
						p.uri = Uri.fromFile(internalFile).toString();
					}
				}
				cupboard().withContext(context).put(Products.CONTENT_URI, p);
			} 
			catch (JSONException e) { 
				//TODO: show error
				return false;
			}
		}
		return true;
	}
	
	private static boolean restoreCategoryWithProducts(Context context, JSONObject categoryJson) {
		// get category data
		try {
			Category category = new Category();
			category.name = categoryJson.getString(DejalistContract.Categories.CATEGORY_NAME);
			category.color = categoryJson.getInt(DejalistContract.Categories.CATEGORY_COLOR);
			if(categoryJson.getLong(DejalistContract.Categories._ID) != Products.PRODUCT_CATEGORY_NONE_ID) {
				// insert the category and get its id
				Uri insertUri = cupboard().withContext(context).put(Categories.CONTENT_URI, category);
				category._id = Categories.getCategoryId(insertUri);
			}
			else category._id = Products.PRODUCT_CATEGORY_NONE_ID;
			restoreProducts(context, categoryJson.getJSONArray(CATEGORY_BACKUP_PRODUCTS_JSON), category._id);
		} catch (JSONException e) {
			//TODO: show error
			return false;
		}
		return true;
	}
	
	private JSONArray readJsonArrayFile(File file) {
		try{
	        FileInputStream fis = new FileInputStream(file);
	        StringBuilder sb = new StringBuilder();
		    {
		        int rc = 0;
		        while((rc = fis.read()) >= 0)
		        {
		            sb.append((char) rc);
		        }
		    }
		    fis.close();
	        return new JSONArray(sb.toString());
	    }
	    catch(JSONException e) {
	    	//TODO: show error
	        return null;
	    }
	    catch(IOException e) {
	    	//TODO: show error
	        return null;
	    }
	}
}