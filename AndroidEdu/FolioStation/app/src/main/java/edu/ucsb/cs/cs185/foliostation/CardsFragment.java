/*
 *  Copyright (c) 2017 - present, Xuan Wang
 *  All rights reserved.
 *
 *  This source code is licensed under the BSD-style license found in the
 *  LICENSE file in the root directory of this source tree.
 *
 */

package edu.ucsb.cs.cs185.foliostation;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.Fragment;

import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.LinearSnapHelper;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SnapHelper;
import android.support.v7.widget.Toolbar;
import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.Element;
import android.support.v8.renderscript.RenderScript;
import android.support.v8.renderscript.ScriptIntrinsicBlur;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;


/**
 * A simple {@link Fragment} subclass.
 */
public class CardsFragment extends Fragment {

    GridCardAdapter mGridCardAdapter;
    RecyclerView mRecyclerView;
    RecyclerView.LayoutManager mLayoutManager;

    public CardsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView =  inflater.inflate(R.layout.fragment_cards, container, false);

        getCardImages();

        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.cards_recycler);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setNestedScrollingEnabled(false);

        GridCardAdapter.setContext(getContext());
        mGridCardAdapter = new GridCardAdapter(ItemCards.cards);
        mGridCardAdapter.setHasStableIds(true);


        mLayoutManager = new GridLayoutManager(getContext(), 2);
        mLayoutManager.setItemPrefetchEnabled(true);

        mRecyclerView.setLayoutManager(mLayoutManager);
        //recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        mGridCardAdapter.setOnItemClickListener(new GridCardAdapter.OnRecyclerViewItemClickListener(){
            @Override
            public void onItemClick(View view , int position){
                Bundle arguments = new Bundle();
                arguments.putInt("POSITION", position);
                DetailBlurDialog fragment = new DetailBlurDialog();

                fragment.setArguments(arguments);
                FragmentManager ft = getActivity().getSupportFragmentManager();

                fragment.show(ft, "dialog");

                Bitmap map=takeScreenShot(getActivity());
                Bitmap fast=BlurBuilder.blur(getContext(), map);
                final Drawable draw=new BitmapDrawable(getResources(),fast);

                ImageView background = (ImageView) getActivity().findViewById(R.id.activity_background);
                background.bringToFront();
                background.setScaleType(ImageView.ScaleType.FIT_XY);
                background.setImageDrawable(draw);

            }
        });

        /*
        SnapHelper helper = new LinearSnapHelper();
        helper.attachToRecyclerView(mRecyclerView);*/


        mRecyclerView.setAdapter(mGridCardAdapter);
        /*mRecyclerView.addOnScrollListener(new ScrollListener(getContext()) {
            @Override
            public void onMoved(int distance) {
                AppCompatActivity activity = (AppCompatActivity) CardsFragment.this.getActivity();
                Toolbar toolbar = (Toolbar) activity.findViewById(R.
                id.toolbar_title);
                toolbar.setTranslationY(-distance);
            }
        });
        */
        return rootView;
    }

    private static Bitmap takeScreenShot(Activity activity)
    {
        View view = activity.getWindow().getDecorView();
        view.setDrawingCacheEnabled(true);
        view.buildDrawingCache();
        Bitmap b1 = view.getDrawingCache();
        Rect frame = new Rect();
        activity.getWindow().getDecorView().getWindowVisibleDisplayFrame(frame);
        int statusBarHeight = frame.top;
        int width = activity.getWindowManager().getDefaultDisplay().getWidth();
        int height = activity.getWindowManager().getDefaultDisplay().getHeight();

        Bitmap b = Bitmap.createBitmap(b1, 0, statusBarHeight, width, height  - statusBarHeight);
        view.destroyDrawingCache();
        return b;
    }

    public static class BlurBuilder {
        private static final float BITMAP_SCALE = 0.3f;
        private static final float BLUR_RADIUS = 24.0f;

        public static Bitmap blur(Context context, Bitmap image) {
            int width = Math.round(image.getWidth() * BITMAP_SCALE);
            int height = Math.round(image.getHeight() * BITMAP_SCALE);

            Bitmap inputBitmap = Bitmap.createScaledBitmap(image, width, height, false);
            Bitmap outputBitmap = Bitmap.createBitmap(inputBitmap);

            RenderScript rs = RenderScript.create(context);
            ScriptIntrinsicBlur theIntrinsic = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
            Allocation tmpIn = Allocation.createFromBitmap(rs, inputBitmap);
            Allocation tmpOut = Allocation.createFromBitmap(rs, outputBitmap);
            theIntrinsic.setRadius(BLUR_RADIUS);
            theIntrinsic.setInput(tmpIn);
            theIntrinsic.forEach(tmpOut);
            tmpOut.copyTo(outputBitmap);

            return outputBitmap;
        }
    }

    private void getCardImages(){
        for(ItemCards.Card card: ItemCards.cards){
            if(!card.mURL.equals("")){
                getImage(card);
            }
        }
    }

    private void getImage(final ItemCards.Card card){
        ImageRequest imageRequest = new ImageRequest(card.mURL,
                new Response.Listener<Bitmap>() {
                    @Override
                    public void onResponse(Bitmap bitmap) {
                        Drawable drawable = new BitmapDrawable(getResources(), bitmap);
                        card.mDrawable = drawable;
                        mGridCardAdapter.notifyDataSetChanged();
                    }
                }, 0, 0, ImageView.ScaleType.CENTER, null,
                new Response.ErrorListener() {
                    public void onErrorResponse(VolleyError error) {
                        Log.e("error", error.toString());
                        //mImageView.setImageResource(R.drawable.image_load_error);
                    }
                });
        // Access the RequestQueue through your singleton class.
        SingletonRequestQueue.getInstance(getContext()).getRequestQueue().add(imageRequest);

    }

    public static Bitmap scale(Bitmap realImage, float maxImageSize,
                               boolean filter) {
        float ratio = Math.min(
                maxImageSize / realImage.getWidth(),
                maxImageSize / realImage.getHeight());
        int width = Math.round( ratio * realImage.getWidth());
        int height = Math.round( ratio * realImage.getHeight());

        Bitmap newBitmap = Bitmap.createScaledBitmap(realImage, width,
                height, filter);
        return newBitmap;
    }

}