package pl.cancellcancer.base.cloudoverlay;

import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import pl.cancellcancer.R;


public class BaseCloudOverlayView<T> extends AbsCloudOverlayView<T>{

    public BaseCloudOverlayView(FrameLayout container) {
        super(container);
    }

    public long getDuration() {
        return 400;
    }

    protected int getCloudViewLayoutId() {
        return R.layout.cloud_view;
    }

    protected int getBoxItemContainerId() {
        return R.id.box_item_container;
    }

    protected int getBoxIndicatorBottomId() {
        return R.id.box_indicator_bottom;
    }

    protected int getBoxIndicatorTopId() {
        return R.id.box_indicator_top;
    }

    protected View prepareView(T item) {
        TextView tv = new TextView(getContext());
        tv.setText(item.toString());
        return tv;
    }
}
