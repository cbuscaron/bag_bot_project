package com.flomio.smartcart.productrecycler;


import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.flomio.smartcart.R;

import java.math.BigDecimal;

import static com.flomio.smartcartlib.util.Logging.logD;

public class ProductsHolder extends ViewHolder<Products> {

    private ProductsHolder(View itemView) {
        super(itemView);
    }

    @SuppressLint("DefaultLocale")
    @Override
    protected void onSetModel(final Products newModel) {
        getPrimaryTextView().setText(getText(newModel));
        getSecondaryTextView().setText(newModel.product.description);

        TextView price = (TextView) getView().findViewById(R.id.productPrice);
        TextView quantity = (TextView) getView().findViewById(R.id
                .productQuantity);

        int numProds = newModel.epcs.size();
        double v = newModel.product.price.multiply
                (new BigDecimal(numProds)).doubleValue();
        price.setText(String.format("$%.2f", v));
        quantity.setText(numProds + " added");

        final ImageView imageView = getImageView();

        if (newModel.bitmap != null) {
            logD("setting bitmap image for: %s", newModel.product.name);
            imageView.setImageBitmap(newModel.bitmap);
            getView().findViewById(R.id.image_load_progress).setVisibility(View.GONE);

//            imageView.setVisibility(View.VISIBLE);
        } else {
//            Resources resources = imageView.getContext().getResources();
//            Drawable drawable = resources.getDrawable(android.R
//                    .drawable.progress_horizontal, resources.newTheme());
//            imageView.setImageDrawable(drawable);
            //imageView.setImageResource(android.R.drawable
              //      .progress_indeterminate_horizontal);
            logD("setting progress image for: %s", newModel.product.name);
        }
    }

    private TextView getPrimaryTextView() {
        return (TextView) getView().findViewById(R.id.text1);
    }
    private TextView getSecondaryTextView() {
        return (TextView) getView().findViewById(R.id.text2);
    }
    private ImageView getImageView() {
        return (ImageView) getView().findViewById(R.id.product_image);
    }

    @SuppressLint("DefaultLocale")
    private String getText(Products newModel) {
        return newModel.product.name;
    }

    public static ProductsHolder make(ViewGroup parent) {
        LayoutInflater viewInflater = LayoutInflater.from(parent.getContext());
        View quoteListItemView = viewInflater.inflate(R.layout
                .products_list_item, parent, false);
        return new ProductsHolder(quoteListItemView);
    }
}
