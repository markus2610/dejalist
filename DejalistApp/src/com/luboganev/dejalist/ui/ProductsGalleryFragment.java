package com.luboganev.dejalist.ui;

import butterknife.InjectView;
import butterknife.Views;

import com.luboganev.dejalist.R;
import com.luboganev.dejalist.data.DejalistContract;
import com.luboganev.dejalist.data.DejalistContract.Products;
import com.luboganev.dejalist.data.entities.Category;

import android.app.Activity;
import android.content.ContentValues;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.Toast;

public class ProductsGalleryFragment extends Fragment implements CategoryActionTaker, LoaderCallbacks<Cursor>, OnItemClickListener, MultiChoiceModeListener {
    public static final String ARG_CATEGORY = "category";
    
    @InjectView(R.id.v_category_colorheader) View categoryColorHeader;
    @InjectView(R.id.grdv_products) GridView mProducts;
    
    private Category mSelectedCategory;
    
    private ProductsGalleryCursorAdapter mAdapter;
    
    private static final int LOADER_PRODUCTS_ID = 2;
    
    private static final String STATE_OPTIONMENUITEMSVISIBLE = "state_optionmenuitemsvisible"; 
    private boolean mOptionMenuItemsVisible; 
    
    public ProductsGalleryFragment() {
        // Empty constructor required for fragment subclasses
    }
    
    CategoryController mCategoryController;
    ProductController mProductController;
    
    public static ProductsGalleryFragment getInstance() {
    	ProductsGalleryFragment fragment = new ProductsGalleryFragment();
    	fragment.setArguments(new Bundle());
    	return fragment;
    }
    
    public static ProductsGalleryFragment getInstance(Category category) {
    	ProductsGalleryFragment fragment = new ProductsGalleryFragment();
    	Bundle bundle = new Bundle();
    	bundle.putParcelable(ARG_CATEGORY, category);
    	fragment.setArguments(bundle);
    	return fragment;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	if(savedInstanceState != null) mOptionMenuItemsVisible = savedInstanceState.getBoolean(STATE_OPTIONMENUITEMSVISIBLE, true);
    	setHasOptionsMenu(true);
    }
    
    @Override
    public void onSaveInstanceState(Bundle outState) {
    	super.onSaveInstanceState(outState);
    	outState.putBoolean(STATE_OPTIONMENUITEMSVISIBLE, mOptionMenuItemsVisible);
    }
    
    @Override
    public void onDestroy() {
    	super.onDestroy();
    	if(mCategoryController != null) {
    		mCategoryController.unregisterCategoryActionTaker();
    	}
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_productsgallery, container, false);
        Views.inject(this, rootView);
        
        if(getArguments().containsKey(ARG_CATEGORY)) {
        	mSelectedCategory = getArguments().getParcelable(ARG_CATEGORY);
        	categoryColorHeader.setBackgroundColor(mSelectedCategory.color);
        	getActivity().setTitle(mSelectedCategory.name);
        } else {
        	mSelectedCategory = null;
        	categoryColorHeader.setVisibility(View.GONE);
        	getActivity().setTitle(R.string.nav_my_products);
        }
        
        return rootView;
    }
    
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    	inflater.inflate( 
    			(getArguments().containsKey(ARG_CATEGORY) ? 
    					R.menu.menu_category : 
    						R.menu.menu_all_products), menu);
    	super.onCreateOptionsMenu(menu, inflater);
    }
    
    @Override
    public void onPrepareOptionsMenu(Menu menu) {
    	super.onPrepareOptionsMenu(menu);
		menu.findItem(R.id.menu_products_sort).setVisible(mOptionMenuItemsVisible);
		menu.findItem(R.id.menu_products_sort).setEnabled(mOptionMenuItemsVisible);
		
		menu.findItem(R.id.menu_new_product).setVisible(mOptionMenuItemsVisible);
		menu.findItem(R.id.menu_new_product).setEnabled(mOptionMenuItemsVisible);
		
		if(getArguments().containsKey(ARG_CATEGORY)) {
			menu.findItem(R.id.menu_categories).setVisible(mOptionMenuItemsVisible);
			menu.findItem(R.id.menu_categories).setEnabled(mOptionMenuItemsVisible);
		}
		else {
			menu.findItem(R.id.menu_categories_new).setVisible(mOptionMenuItemsVisible);
			menu.findItem(R.id.menu_categories_new).setEnabled(mOptionMenuItemsVisible);
		}
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_products_sort_az:
        	if (item.isChecked()) item.setChecked(false);
            else item.setChecked(true);
            Toast.makeText(getActivity(), "Clicked: sort az", Toast.LENGTH_SHORT).show();
            return true;
        case R.id.menu_products_sort_recent:
        	if (item.isChecked()) item.setChecked(false);
            else item.setChecked(true);
            Toast.makeText(getActivity(), "Clicked: sort recent", Toast.LENGTH_SHORT).show();
            return true;   
        case R.id.menu_products_sort_usage:
            if (item.isChecked()) item.setChecked(false);
            else item.setChecked(true);
            Toast.makeText(getActivity(), "Clicked: sort usage", Toast.LENGTH_SHORT).show();
            return true;
        case R.id.menu_new_product:
            if(mProductController != null) mProductController.newProduct(mSelectedCategory);
            return true;  
        case R.id.menu_categories_new:
        	if(mCategoryController != null) mCategoryController.newCategory();
            return true;
        case R.id.menu_categories_edit:
        	if(mCategoryController != null) mCategoryController.editCategory(mSelectedCategory);
            return true;   
        case R.id.menu_categories_delete:
        	if(mCategoryController != null) mCategoryController.deleteCategory(mSelectedCategory);
            return true;  
        default:
            return super.onOptionsItemSelected(item);
        }
    }
    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
        	mCategoryController = (CategoryController) activity;
        	mCategoryController.registerCategoryActionTaker(this);
        	
        	mProductController = (ProductController) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement CategoriesController");
        }
        try {
        	mProductController = (ProductController) activity;
        } catch (ClassCastException e) {
        	// The activity doesn't implement the interface, throw exception
        	throw new ClassCastException(activity.toString()
        			+ " must implement ProductController");
        }
    }
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
    	super.onActivityCreated(savedInstanceState);
    	
        mAdapter = new ProductsGalleryCursorAdapter(getActivity().getApplicationContext(), 
        		CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER, mSelectedCategory == null);
        mProducts.setAdapter(mAdapter);
        mProducts.setOnItemClickListener(this);
        mProducts.setChoiceMode(GridView.CHOICE_MODE_MULTIPLE_MODAL);
        mProducts.setMultiChoiceModeListener(this);
    	
        if(getActivity().getSupportLoaderManager().getLoader(LOADER_PRODUCTS_ID) != null) {
        	getActivity().getSupportLoaderManager().restartLoader(LOADER_PRODUCTS_ID, null, this);
        }
        else getActivity().getSupportLoaderManager().initLoader(LOADER_PRODUCTS_ID, null, this);
    }

	@Override
	public void updateShownCategory(Category category) {
		if(mSelectedCategory == null) return;
		if(mSelectedCategory._id == category._id) {
			mSelectedCategory = category;
			categoryColorHeader.setBackgroundColor(mSelectedCategory.color);
			getActivity().setTitle(mSelectedCategory.name);
		}
	}

	@Override
	public void setOptionMenuItemsVisible(boolean visible) {
		mOptionMenuItemsVisible = visible;
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		if(mSelectedCategory != null) {
			return new CursorLoader(getActivity().getApplicationContext(), 
					DejalistContract.Products.buildCategoryProductsUri(mSelectedCategory._id), null, null, null, null);
		}
		else {
			return new CursorLoader(getActivity().getApplicationContext(), 
					DejalistContract.Products.CONTENT_URI, null, null, null, null);
		}
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		data.setNotificationUri(getActivity().getContentResolver(), DejalistContract.Products.CONTENT_URI);
		mAdapter.changeCursor(data);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mAdapter.changeCursor(null);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		ProductsGalleryCursorAdapter.ViewHolder holder = (ProductsGalleryCursorAdapter.ViewHolder)view.getTag();
		ContentValues values = new ContentValues();
		values.put(Products.PRODUCT_INLIST, holder.inList.getVisibility() == View.VISIBLE ? 0 : 1);
		getActivity().getContentResolver().update(Products.buildProductUri(id), values, null, null);
	}

	@Override
	public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
		// Respond to clicks on the actions in the CAB
        switch (item.getItemId()) {
//            case R.id.menu_cab_products_edit:
//                deleteSelectedItems();
//                mode.finish(); // Action picked, so close the CAB
//                return true;
//            case R.id.menu_cab_products_set_category:
//                deleteSelectedItems();
//                mode.finish(); // Action picked, so close the CAB
//                return true;
//            case R.id.menu_cab_products_delete:
//                deleteSelectedItems();
//                mode.finish(); // Action picked, so close the CAB
//                return true;
            default:
                return false;
        }
	}

	@Override
	public boolean onCreateActionMode(ActionMode mode, Menu menu) {
		// Inflate the menu for the CAB
        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.menu_cab_products, menu);
        return true;
	}

	@Override
	public void onDestroyActionMode(ActionMode mode) {
		 // Here you can make any necessary updates to the activity when
        // the CAB is removed. By default, selected items are deselected/unchecked.
	}

	@Override
	public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
		// Here you can perform updates to the CAB due to
        // an invalidate() request
		MenuItem editItem = mode.getMenu().findItem(R.id.menu_cab_products_edit);
		MenuItem setCategoryItem = mode.getMenu().findItem(R.id.menu_cab_products_set_category);
    	if(mProducts.getCheckedItemCount() == 1) {
    		editItem.setVisible(true);
    		editItem.setEnabled(true);
    		setCategoryItem.setVisible(false);
    		setCategoryItem.setEnabled(false);
    	}
    	else {
    		editItem.setVisible(false);
    		editItem.setEnabled(false);
    		setCategoryItem.setVisible(true);
    		setCategoryItem.setEnabled(true);
    	}
        return true;
	}

	@Override
	public void onItemCheckedStateChanged(ActionMode mode, int position,
			long id, boolean checked) {
		// Here you can do something when items are selected/de-selected,
        // such as update the title in the CAB
		int count = mProducts.getCheckedItemCount();
		if(checked && count == 2) mode.invalidate();
		else if (count == 1) mode.invalidate();
		Resources res = getResources();
    	String text = String.format(res.getString(R.string.menu_cab_products_title), mProducts.getCheckedItemCount());
    	mode.setTitle(text);
	}
}
