/*
 * 	Copyright (c) 2017. Token Browser, Inc
 *
 * 	This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.tokenbrowser.view.custom;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;

import com.tokenbrowser.R;
import com.tokenbrowser.view.adapter.StarAdapter;
import com.tokenbrowser.view.adapter.listeners.OnItemClickListener;

public class StarRatingView extends RecyclerView {

    private boolean bigMode;

    public StarRatingView(Context context) {
        super(context);
        init();
    }

    public StarRatingView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        parseAttributeSet(context, attrs);
        init();
    }

    public StarRatingView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        parseAttributeSet(context, attrs);
        init();
    }

    private void parseAttributeSet(final Context context, final @Nullable AttributeSet attrs) {
        final TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.StarRating, 0, 0);
        this.bigMode = a.getBoolean(R.styleable.StarRating_bigMode, false);
        a.recycle();
    }

    private void init() {
        inflate(getContext(), R.layout.view_star_rating, this);
        initRecyclerView();
    }

    private void initRecyclerView() {
        final int spacing = this.getResources().getDimensionPixelSize(R.dimen.star_right_margin);
        this.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        this.addItemDecoration(new SpaceDecoration(spacing));
        this.setAdapter(new StarAdapter(this.bigMode));
    }

    public void setOnItemClickListener(final OnItemClickListener<Integer> listener) {
        final StarAdapter adapter = (StarAdapter) this.getAdapter();
        adapter.setOnItemClickListener(listener);
    }

    public void setStars(final Double reputationScore) {
        if (this.getAdapter() == null) return;
        final double rating = reputationScore != null
                ? reputationScore
                : 0;
        final StarAdapter adapter = (StarAdapter) this.getAdapter();
        adapter.setStars(rating);
    }
}