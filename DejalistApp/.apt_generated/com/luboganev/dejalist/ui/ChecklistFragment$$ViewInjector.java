// Generated code from Butter Knife. Do not modify!
package com.luboganev.dejalist.ui;

import android.view.View;
import butterknife.Views.Finder;

public class ChecklistFragment$$ViewInjector {
  public static void inject(Finder finder, com.luboganev.dejalist.ui.ChecklistFragment target, Object source) {
    View view;
    view = finder.findById(source, 2131361821);
    target.mEmptyImage = (android.widget.ImageView) view;
    view = finder.findById(source, 2131361823);
    target.mProducts = (android.widget.ListView) view;
    view = finder.findById(source, 2131361822);
    target.mEmptyText = (android.widget.TextView) view;
    view = finder.findById(source, 2131361824);
    target.mAddProducts = (android.widget.ImageButton) view;
  }

  public static void reset(com.luboganev.dejalist.ui.ChecklistFragment target) {
    target.mEmptyImage = null;
    target.mProducts = null;
    target.mEmptyText = null;
    target.mAddProducts = null;
  }
}
