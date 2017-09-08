package pl.cancellcancer.base.cloudoverlay;

import android.animation.Animator;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import java.util.List;

import pl.amistad.util.view.DoWhenLayouted;

public abstract class AbsCloudOverlayView<T> {

    private final LayoutInflater inflater;
    private final FrameLayout container;
    private OnItemSelectedListener<T> onItemSelectedListener;

    public AbsCloudOverlayView(FrameLayout container) {
        this.container = container;
        inflater = LayoutInflater.from(container.getContext());
    }

    public long getDuration() {
        return 400;
    }

    public LayoutInflater getInflater() {
        return inflater;
    }

    private static class CloudBox {
        private final View boxIndicatorBottom;
        private final View boxIndicatorTop;
        private final View cloudBoxView;
        private final ViewGroup box;
        private final FrameLayout container;

        private CloudBox(
                FrameLayout container,
                View cloudBoxView,
                int boxItemContainerId,
                int boxIndicatorBottomId,
                int boxIndicatorTopId

        ) {
            this.cloudBoxView = cloudBoxView;
            this.container = container;
            box = cloudBoxView.findViewById(boxItemContainerId);
            boxIndicatorBottom = cloudBoxView.findViewById(boxIndicatorBottomId);
            boxIndicatorTop = cloudBoxView.findViewById(boxIndicatorTopId);
        }
    }

    protected abstract int getCloudViewLayoutId();

    protected abstract int getBoxItemContainerId();

    protected abstract int getBoxIndicatorBottomId();

    protected abstract int getBoxIndicatorTopId();

    protected abstract View prepareView(T item);

    protected Drawable getDividerDrawable() {
        return null;
    }

    public Context getContext() {
        return container.getContext();
    }

    private CloudBox createCloudBox() {
        View cloudBoxView = inflater.inflate(getCloudViewLayoutId(), container, false);
        container.addView(cloudBoxView);
        return new CloudBox(
                container,
                cloudBoxView,
                getBoxItemContainerId(),
                getBoxIndicatorBottomId(),
                getBoxIndicatorTopId()
        );
    }

    public Cloud<T> showView(final int xx, final int y, List<T> items) {
        final CloudBox cloudBox = createCloudBox();
        Cloud<T> cloud = new Cloud<>(cloudBox, this);
        updateView(cloud, xx, y, items);
        return cloud;
    }

    public void removeView(Cloud cloud) {
        cloud.cloudBox.box.removeAllViews();
    }

    private void updateView(final Cloud cloud, final int xx, final int y, List<T> items) {
        cloud.cloudBox.box.removeAllViews();

        cloud.setPosition(xx, y);

        for (int i = 0; i < items.size(); i++) {
            T item = items.get(i);
            View view = prepareView(item);
            view.setOnClickListener(new OnItemClickListener(cloud, item));
            cloud.cloudBox.box.addView(view);
            if (getDividerDrawable() != null) {
                if(cloud.cloudBox.box instanceof LinearLayout) {
                    ((LinearLayout)cloud.cloudBox.box).setDividerDrawable(getDividerDrawable());
                    ((LinearLayout)cloud.cloudBox.box).setShowDividers(LinearLayout.SHOW_DIVIDER_MIDDLE);
                }
            }
        }

        if (cloud.onLayoutChangeListener != null) {
            cloud.cloudBox.box.removeOnLayoutChangeListener(cloud.onLayoutChangeListener);
        }

        cloud.onLayoutChangeListener = new View.OnLayoutChangeListener() {
            boolean firstTime = true;

            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                final boolean firstTimeFinal = firstTime;
                firstTime = false;
                DoWhenLayouted.doWhenLayouted(
                        cloud.cloudBox.cloudBoxView,
                        cloud.cloudBox.boxIndicatorTop,
                        cloud.cloudBox.boxIndicatorBottom,
                        new DoWhenLayouted.Action3() {
                            @Override
                            public void doOnView(View view1, View view2, View view3) {
                                if (firstTimeFinal) {
                                    cloud.cloudBox.cloudBoxView.setAlpha(0);
                                    cloud.cloudBox.cloudBoxView.animate()
                                            .alpha(1)
                                            .setDuration(getDuration())
                                            .start();
                                }
                                cloud.updatePosition(cloud.currentX, cloud.currentY);
                                cloud.updatePosition(cloud.currentX, cloud.currentY, true);
                            }
                        }
                );
            }
        };

        cloud.cloudBox.box.addOnLayoutChangeListener(cloud.onLayoutChangeListener);
    }

    private void updateViewSmooth(CloudBox cloudBox, int xx, int y) {
        int bWidth = cloudBox.box.getWidth();
        int bHeight = cloudBox.box.getHeight();

        int bi2Height = cloudBox.boxIndicatorTop.getHeight();
        int bi2Width = cloudBox.boxIndicatorTop.getWidth();

        int biWidth = cloudBox.boxIndicatorBottom.getWidth();
        int biHeight = cloudBox.boxIndicatorBottom.getHeight();

        String log = "bWidth = " + bWidth + ", " +
                "bHeight = " + bHeight + ", " +
                "bi2Height = " + bi2Height + ", " +
                "bi2Width = " + bi2Width + ", " +
                "biWidth = " + biWidth + ", " +
                "biHeight = " + biHeight + ", ";
        Log.d("UPDATE", log);

        float delta = 0;//cloudBox.container.getResources().getDisplayMetrics().density * 20;

        if (y < bHeight + biHeight) {
            cloudBox.boxIndicatorTop.setAlpha(1);
            cloudBox.boxIndicatorBottom.setAlpha(0);

            float x = xx - bWidth / 2;
            if (x < 0) {
                if (x > 0 - bWidth / 2 + biWidth / 2) {
                    x = 0;
                } else {
                    x = x + bWidth / 2 - biWidth / 2;
                }
            }
            if (x + bWidth > cloudBox.container.getWidth()) {
                if (x < cloudBox.container.getWidth() - bWidth / 2 - biWidth / 2) {
                    x = cloudBox.container.getWidth() - bWidth;
                } else {
                    x = x - bWidth / 2 + biWidth / 2;
                }
            }

            cloudBox.box.animate()
                    .translationX(x)
                    .translationY(y)
                    .setDuration(getDuration()).start();
            cloudBox.boxIndicatorTop.animate()
                    .translationX(xx - bi2Width / 2)
                    .translationY(y)
                    .setDuration(getDuration())
                    .start();
            cloudBox.boxIndicatorBottom.animate()
                    .translationX(xx - biWidth / 2).translationY(y - delta).setDuration(getDuration()).start();
        } else {
            cloudBox.boxIndicatorBottom.setAlpha(1);
            cloudBox.boxIndicatorTop.setAlpha(0);

            float x = xx - bWidth / 2;
            if (x < 0) {
                if (x > 0 - bWidth / 2 + biWidth / 2) {
                    x = 0;
                } else {
                    x = x + bWidth / 2 - biWidth / 2;
                }
            }

            if (x + bWidth > cloudBox.container.getWidth()) {
                if (x < cloudBox.container.getWidth() - bWidth / 2 - biWidth / 2) {
                    x = cloudBox.container.getWidth() - bWidth;
                } else {
                    x = x - bWidth / 2 + biWidth / 2;
                }
            }

            cloudBox.box.animate()
                    .translationX(x)
                    .translationY(y - bHeight - biHeight - bi2Height)
                    .setDuration(getDuration())
                    .start();
            cloudBox.boxIndicatorTop.animate()
                    .translationX(xx - bi2Width / 2)
                    .translationY(y - bHeight - biHeight - bi2Height)
                    .setDuration(getDuration())
                    .start();
            cloudBox.boxIndicatorBottom.animate()
                    .translationX(xx - biWidth / 2)
                    .translationY(y - biHeight - bHeight - bi2Height - delta)
                    .setDuration(getDuration())
                    .start();
        }
    }

    private void updateView(final CloudBox cloudBox, final int xx, final int y) {
        cloudBox.cloudBoxView.setVisibility(View.VISIBLE);

        int bWidth = cloudBox.box.getWidth();
        int bHeight = cloudBox.box.getHeight();

        int bi2Height = cloudBox.boxIndicatorTop.getHeight();
        int bi2Width = cloudBox.boxIndicatorTop.getWidth();

        int biWidth = cloudBox.boxIndicatorBottom.getWidth();
        int biHeight = cloudBox.boxIndicatorBottom.getHeight();

        float delta = 0;

        if (y < bHeight + biHeight) {
            cloudBox.boxIndicatorTop.setAlpha(1);
            cloudBox.boxIndicatorBottom.setAlpha(0);

            float x = xx - bWidth / 2;
            if (x < 0) {
                if (x > 0 - bWidth / 2 + biWidth / 2) {
                    x = 0;
                } else {
                    x = x + bWidth / 2 - biWidth / 2;
                }
            }
            if (x + bWidth > container.getWidth()) {
                if (x < container.getWidth() - bWidth / 2 - biWidth / 2) {
                    x = container.getWidth() - bWidth;
                } else {
                    x = x - bWidth / 2 + biWidth / 2;
                }
            }

            cloudBox.box.setTranslationX(x);
            cloudBox.box.setTranslationY(y);

            cloudBox.boxIndicatorTop.setTranslationX(xx - bi2Width / 2);
            cloudBox.boxIndicatorTop.setTranslationY(y);

            cloudBox.boxIndicatorBottom.setTranslationX(xx - biWidth / 2);
            cloudBox.boxIndicatorBottom.setTranslationY(y - delta);
        } else {
            cloudBox.boxIndicatorBottom.setAlpha(1);
            cloudBox.boxIndicatorTop.setAlpha(0);

            float x = xx - bWidth / 2;
            if (x < 0) {
                if (x > 0 - bWidth / 2 + biWidth / 2) {
                    x = 0;
                } else {
                    x = x + bWidth / 2 - biWidth / 2;
                }
            }
            if (x + bWidth > cloudBox.container.getWidth()) {
                if (x < cloudBox.container.getWidth() - bWidth / 2 - biWidth / 2) {
                    x = cloudBox.container.getWidth() - bWidth;
                } else {
                    x = x - bWidth / 2 + biWidth / 2;
                }
            }

            cloudBox.box.setTranslationX(x);
            cloudBox.box.setTranslationY(y - bHeight - biHeight - bi2Height);

            cloudBox.boxIndicatorTop.setTranslationX(xx - bi2Width / 2);
            cloudBox.boxIndicatorTop.setTranslationY(y - bHeight - biHeight - bi2Height);

            cloudBox.boxIndicatorBottom.setTranslationX(xx - biWidth / 2);
            cloudBox.boxIndicatorBottom.setTranslationY(y - biHeight - bHeight - bi2Height - delta);
        }
    }

    private void hideView(final CloudBox cloudBox) {
        AnimationFinishedListener animationFinishedListener = new AnimationFinishedListener() {
            @Override
            public void onAnimationEnd(Animator animation) {
                cloudBox.container.removeView(cloudBox.cloudBoxView);
            }
        };
        cloudBox.cloudBoxView.setAlpha(1);
        cloudBox.cloudBoxView.animate()
                .alpha(0).setDuration(getDuration()).setListener(animationFinishedListener).start();
    }

    public static class Cloud<T> {
        CloudBox cloudBox;
        AbsCloudOverlayView<T> cloudOverlayView;
        View.OnLayoutChangeListener onLayoutChangeListener;
        boolean isRemoved = false;
        int currentX = 0;
        int currentY = 0;

        Cloud(CloudBox cloudBox, AbsCloudOverlayView<T> cloudOverlayView) {
            this.cloudBox = cloudBox;
            this.cloudOverlayView = cloudOverlayView;
        }

        public void remove() {
            if (isRemoved)
                throw new IllegalStateException("This cloud is already removed, you can't remove it again!");

            isRemoved = true;
            cloudOverlayView.hideView(cloudBox);
        }

        public boolean isRemoved() {
            return isRemoved;
        }

        public void updatePosition(int x, int y) {
            if (isRemoved)
                throw new IllegalStateException("This cloud is already removed, you can't update it's position!");

            updatePosition(x, y, false);
        }

        public void updatePosition(int x, int y, boolean smooth) {
            currentX = x;
            currentY = y;
            if (isRemoved)
                throw new IllegalStateException("This cloud is already removed, you can't update it's position!");

            if (smooth) {
                cloudOverlayView.updateViewSmooth(cloudBox, x, y);
            } else {
                cloudOverlayView.updateView(cloudBox, x, y);
            }
        }

        public void setItems(List<T> items) {
            cloudOverlayView.updateView(this, currentX, currentY, items);
        }

        public void setPosition(int xx, int y) {
            currentX = xx;
            currentY = y;
        }
    }

    public void setOnItemSelectedListener(OnItemSelectedListener<T> onItemSelectedListener) {
        this.onItemSelectedListener = onItemSelectedListener;
    }

    public interface OnItemSelectedListener<T> {
        void onItemSelected(Cloud<T> cloud, T item);
    }

    private class OnItemClickListener implements View.OnClickListener {

        Cloud cloud;
        private T item;

        OnItemClickListener(Cloud cloud, T item) {
            this.item = item;
            this.cloud = cloud;
        }

        @Override
        public void onClick(View v) {
            if (onItemSelectedListener != null) {
                if (!cloud.isRemoved) {
                    onItemSelectedListener.onItemSelected(cloud, item);
                }
            }
        }
    }

    abstract class AnimationFinishedListener implements Animator.AnimatorListener {

        @Override
        public void onAnimationStart(Animator animation) {

        }

        @Override
        public void onAnimationCancel(Animator animation) {

        }

        @Override
        public void onAnimationRepeat(Animator animation) {

        }
    }
}
