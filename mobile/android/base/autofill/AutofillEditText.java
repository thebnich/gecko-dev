package org.mozilla.gecko.autofill;

import org.mozilla.gecko.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.EditText;

public class AutofillEditText extends EditText {
    private String fieldName;

    public AutofillEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        initAttrs(context, attrs);
    }

    public AutofillEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initAttrs(context, attrs);
    }

    private void initAttrs(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.AutofillEditText);
        try {
            fieldName = a.getString(R.styleable.AutofillEditText_autofillField);
            if (fieldName == null) {
                throw new RuntimeException("You must supply a fieldName attribute.");
            }
        } finally {
            a.recycle();
        }
    }

    public String getFieldName() {
        return fieldName;
    }
}
