// Generated code from Butter Knife. Do not modify!
package com.luboganev.dejalist.ui;

import android.view.View;
import butterknife.Views.Finder;

public class MainActivity$$ViewInjector {
  public static void inject(Finder finder, final com.luboganev.dejalist.ui.MainActivity target, Object source) {
    View view;
    view = finder.findById(source, 2131492878);
    if (view == null) {
      throw new IllegalStateException("Required view with id '2131492878' for field 'mDrawerList' was not found. If this field binding is optional add '@Optional'.");
    }
    target.mDrawerList = (android.widget.ListView) view;
    view = finder.findById(source, 2131492873);
    if (view == null) {
      throw new IllegalStateException("Required view with id '2131492873' for field 'mDrawerLayout' was not found. If this field binding is optional add '@Optional'.");
    }
    target.mDrawerLayout = (android.support.v4.widget.DrawerLayout) view;
  }

  public static void reset(com.luboganev.dejalist.ui.MainActivity target) {
    target.mDrawerList = null;
    target.mDrawerLayout = null;
  }
}
