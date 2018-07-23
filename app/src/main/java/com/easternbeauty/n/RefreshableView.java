package com.easternbeauty.n;

import android.content.Context;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewConfiguration;
import android.view.animation.RotateAnimation;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

public class RefreshableView extends LinearLayout implements OnTouchListener {

	/**
	 * ����״̬
	 */
	public static final int STATUS_PULL_TO_REFRESH = 0;

	/**
	 * �ͷ�����ˢ��״̬
	 */
	public static final int STATUS_RELEASE_TO_REFRESH = 1;

	/**
	 * ����ˢ��״̬
	 */
	public static final int STATUS_REFRESHING = 2;

	/**
	 * ˢ����ɻ�δˢ��״̬
	 */
	public static final int STATUS_REFRESH_FINISHED = 3;

	/**
	 * ����ͷ���ع����ٶ�
	 */
	public static final int SCROLL_SPEED = -20;

	/**
	 * һ���ӵĺ���ֵ�������ж��ϴεĸ���ʱ��
	 */
	public static final long ONE_MINUTE = 60 * 1000;

	/**
	 * һСʱ�ĺ���ֵ�������ж��ϴεĸ���ʱ��
	 */
	public static final long ONE_HOUR = 60 * ONE_MINUTE;

	/**
	 * һ��ĺ���ֵ�������ж��ϴεĸ���ʱ��
	 */
	public static final long ONE_DAY = 24 * ONE_HOUR;

	/**
	 * һ�µĺ���ֵ�������ж��ϴεĸ���ʱ��
	 */
	public static final long ONE_MONTH = 30 * ONE_DAY;

	/**
	 * һ��ĺ���ֵ�������ж��ϴεĸ���ʱ��
	 */
	public static final long ONE_YEAR = 12 * ONE_MONTH;


	/**
	 * ����ˢ�µĻص��ӿ�
	 */
	private PullToRefreshListener mListener;

	/**
	 * ���ڴ洢�ϴθ���ʱ��
	 */
//	private SharedPreferences preferences;

	/**
	 * ����ͷ��View
	 */
	private View header;

	/**
	 * ��Ҫȥ����ˢ�µ�ListView
	 */
	private GridView listView;

	/**
	 * ˢ��ʱ��ʾ�Ľ�����
	 */
	private ProgressBar progressBar;

	/**
	 * ָʾ�������ͷŵļ�ͷ
	 */
	private ImageView arrow;

	/**
	 * ָʾ�������ͷŵ���������
	 */
	private TextView description;

	/**
	 * ����ͷ�Ĳ��ֲ���
	 */
	private MarginLayoutParams headerLayoutParams;

	/**
	 * Ϊ�˷�ֹ��ͬ���������ˢ�����ϴθ���ʱ���ϻ����г�ͻ��ʹ��id��������
	 */
	@SuppressWarnings("unused")
	private int mId = -1;

	/**
	 * ����ͷ�ĸ߶�
	 */
	private int hideHeaderHeight;

	/**
	 * ��ǰ����ʲô״̬����ѡֵ��STATUS_PULL_TO_REFRESH, STATUS_RELEASE_TO_REFRESH,
	 * STATUS_REFRESHING �� STATUS_REFRESH_FINISHED
	 */
	private int currentStatus = STATUS_REFRESH_FINISHED;;

	/**
	 * ��¼��һ�ε�״̬��ʲô����������ظ�����
	 */
	private int lastStatus = currentStatus;

	/**
	 * ��ָ����ʱ����Ļ������
	 */
	private float yDown;

	/**
	 * �ڱ��ж�Ϊ����֮ǰ�û���ָ�����ƶ������ֵ��
	 */
	private int touchSlop;

	/**
	 * �Ƿ��Ѽ��ع�һ��layout������onLayout�еĳ�ʼ��ֻ�����һ��
	 */
	private boolean loadOnce;

	/**
	 * ��ǰ�Ƿ����������ֻ��ListView������ͷ��ʱ�����������
	 */
	private boolean ableToPull;
	
	// Top margin for the grid view (in pixel)
	private int topMargin;

	/**
	 * ����ˢ�¿ؼ��Ĺ��캯������������ʱ��̬���һ������ͷ�Ĳ��֡�
	 * 
	 * @param context
	 * @param attrs
	 */
	public RefreshableView(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.topMargin = convertDiptoPx(4);
		//debug("topMargin=%d", topMargin);
		//preferences = PreferenceManager.getDefaultSharedPreferences(context);
		header = LayoutInflater.from(context).inflate(R.layout.pull_to_refresh,
				null, false);
		progressBar = (ProgressBar) header.findViewById(R.id.progress_bar);
		arrow = (ImageView) header.findViewById(R.id.arrow);
		description = (TextView) header.findViewById(R.id.description);
		touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
		setOrientation(VERTICAL);
		addView(header, 0);
	}
	
	public int convertDiptoPx(int pixel){
		float scale = getResources().getDisplayMetrics().density;
	    int dips=(int) ((pixel * scale) + 0.5f);
	    return dips;
	}

	/**
	 * ����һЩ�ؼ��Եĳ�ʼ�����������磺������ͷ����ƫ�ƽ������أ���ListViewע��touch�¼���
	 */
	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		super.onLayout(changed, l, t, r, b);
		if (changed && !loadOnce) {
			hideHeaderHeight = -header.getHeight();
			headerLayoutParams = (MarginLayoutParams) header.getLayoutParams();
			headerLayoutParams.topMargin = hideHeaderHeight;
			listView = (GridView) getChildAt(1);
			listView.setOnTouchListener(this);
			loadOnce = true;
		}
	}

	/**
	 * ��ListView������ʱ���ã����д����˸�������ˢ�µľ����߼���
	 */
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		setIsAbleToPull(event);
		//debug("ableToPull=" + ableToPull);
		if (ableToPull) {
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				yDown = event.getRawY();
				break;
			case MotionEvent.ACTION_MOVE:
				float yMove = event.getRawY();
				int distance = (int) (yMove - yDown);
				// �����ָ���»�״̬����������ͷ����ȫ���صģ������������¼�
				if (distance <= 0
						&& headerLayoutParams.topMargin <= hideHeaderHeight) {
					return false;
				}
				if (distance < touchSlop) {
					return false;
				}
				if (currentStatus != STATUS_REFRESHING) {
					if (headerLayoutParams.topMargin > 0) {
						currentStatus = STATUS_RELEASE_TO_REFRESH;
					} else {
						currentStatus = STATUS_PULL_TO_REFRESH;
					}
					// ͨ��ƫ������ͷ��topMarginֵ����ʵ������Ч��
					headerLayoutParams.topMargin = (distance / 2)
							+ hideHeaderHeight;
					header.setLayoutParams(headerLayoutParams);
				}
				break;
			case MotionEvent.ACTION_UP:
			default:
				if (currentStatus == STATUS_RELEASE_TO_REFRESH) {
					// ����ʱ������ͷ�����ˢ��״̬����ȥ��������ˢ�µ�����
					new RefreshingTask().execute();
				} else if (currentStatus == STATUS_PULL_TO_REFRESH) {
					// ����ʱ���������״̬����ȥ������������ͷ������
					new HideHeaderTask().execute();
				}
				break;
			}
			// ʱ�̼ǵø�������ͷ�е���Ϣ
			if (currentStatus == STATUS_PULL_TO_REFRESH
					|| currentStatus == STATUS_RELEASE_TO_REFRESH) {
				updateHeaderView();
				// ��ǰ�������������ͷ�״̬��Ҫ��ListViewʧȥ���㣬���򱻵������һ���һֱ����ѡ��״̬
//				listView.setPressed(false);
//				listView.setFocusable(false);
//				listView.setFocusableInTouchMode(false);
				lastStatus = currentStatus;
				// ��ǰ�������������ͷ�״̬��ͨ������true���ε�ListView�Ĺ����¼�
				return true;
			}
		}
		return false;
	}

	@SuppressWarnings("unused")
	private void debug(String format, Object... args) {
		String msg = String.format(format, args);
		Log.d("DEBUG", msg);
	}

	/**
	 * ������ˢ�¿ؼ�ע��һ����������
	 * 
	 * @param listener
	 *            ��������ʵ�֡�
	 * @param id
	 *            Ϊ�˷�ֹ��ͬ���������ˢ�����ϴθ���ʱ���ϻ����г�ͻ�� �벻ͬ������ע������ˢ�¼�����ʱһ��Ҫ���벻ͬ��id��
	 */
	public void setOnRefreshListener(PullToRefreshListener listener, int id) {
		mListener = listener;
		mId = id;
	}

	/**
	 * �����е�ˢ���߼���ɺ󣬼�¼����һ�£��������ListView��һֱ��������ˢ��״̬��
	 */
	public void finishRefreshing() {
		currentStatus = STATUS_REFRESH_FINISHED;
		new HideHeaderTask().execute();
	}

	/**
	 * ���ݵ�ǰListView�Ĺ���״̬���趨 {@link #ableToPull}
	 * ��ֵ��ÿ�ζ���Ҫ��onTouch�е�һ��ִ�У����������жϳ���ǰӦ���ǹ���ListView������Ӧ�ý���������
	 * 
	 * @param event
	 */
	private void setIsAbleToPull(MotionEvent event) {
		View firstChild = listView.getChildAt(0);
		if (firstChild != null) {
			int firstVisiblePos = listView.getFirstVisiblePosition();
			//debug("firstVisiblePos=%d, firstChild.getTop()=%d", firstVisiblePos, firstChild.getTop());
			if (firstVisiblePos == 0 && firstChild.getTop() == topMargin) {
				if (!ableToPull) {
					yDown = event.getRawY();
				}
				// ����׸�Ԫ�ص��ϱ�Ե�����븸����ֵΪ0����˵��ListView���������������ʱӦ����������ˢ��
				ableToPull = true;
			} else {
				if (headerLayoutParams.topMargin != hideHeaderHeight) {
					headerLayoutParams.topMargin = hideHeaderHeight;
					header.setLayoutParams(headerLayoutParams);
				}
				ableToPull = false;
			}
		} else {
			// ���ListView��û��Ԫ�أ�ҲӦ����������ˢ��
			ableToPull = true;
		}
	}

	/**
	 * ��������ͷ�е���Ϣ��
	 */
	private void updateHeaderView() {
		if (lastStatus != currentStatus) {
			if (currentStatus == STATUS_PULL_TO_REFRESH) {
				description.setText(getResources().getString(
						R.string.pull_to_refresh));
				arrow.setVisibility(View.VISIBLE);
				progressBar.setVisibility(View.GONE);
				rotateArrow();
			} else if (currentStatus == STATUS_RELEASE_TO_REFRESH) {
				description.setText(getResources().getString(
						R.string.release_to_refresh));
				arrow.setVisibility(View.VISIBLE);
				progressBar.setVisibility(View.GONE);
				rotateArrow();
			} else if (currentStatus == STATUS_REFRESHING) {
				description.setText(getResources().getString(
						R.string.refreshing));
				progressBar.setVisibility(View.VISIBLE);
				arrow.clearAnimation();
				arrow.setVisibility(View.GONE);
			}
		}
	}

	/**
	 * ���ݵ�ǰ��״̬����ת��ͷ��
	 */
	private void rotateArrow() {
		//debug("rotateArrow called");
		float pivotX = arrow.getWidth() / 2f;
		float pivotY = arrow.getHeight() / 2f;
		float fromDegrees = 0f;
		float toDegrees = 0f;
		if (currentStatus == STATUS_PULL_TO_REFRESH) {
			fromDegrees = 180f;
			toDegrees = 360f;
		} else if (currentStatus == STATUS_RELEASE_TO_REFRESH) {
			fromDegrees = 0f;
			toDegrees = 180f;
		}
		RotateAnimation animation = new RotateAnimation(fromDegrees, toDegrees,
				pivotX, pivotY);
		animation.setDuration(100);
		animation.setFillAfter(true);
		arrow.startAnimation(animation);
	}

	/**
	 * ����ˢ�µ������ڴ������л�ȥ�ص�ע�����������ˢ�¼�������
	 * 
	 * @author guolin
	 */
	class RefreshingTask extends AsyncTask<Void, Integer, Integer> {

		@Override
		protected Integer doInBackground(Void... params) {
			int topMargin = headerLayoutParams.topMargin;
			while (true) {
				topMargin = topMargin + SCROLL_SPEED;
				if (topMargin <= 0) {
					topMargin = 0;
					break;
				}
				publishProgress(topMargin);
				sleep(10);
			}
			currentStatus = STATUS_REFRESHING;
			publishProgress(0);
			if (mListener != null) {
				mListener.onRefresh();
			}
			return null;
		}

		@Override
		protected void onProgressUpdate(Integer... topMargin) {
			updateHeaderView();
			headerLayoutParams.topMargin = topMargin[0];
			header.setLayoutParams(headerLayoutParams);
		}

		@Override
		protected void onPostExecute(Integer result) {
			if (mListener != null) {
				mListener.onFinish();
			}
		}
	}

	/**
	 * ��������ͷ�����񣬵�δ��������ˢ�»�����ˢ����ɺ󣬴����񽫻�ʹ����ͷ�������ء�
	 * 
	 * @author guolin
	 */
	class HideHeaderTask extends AsyncTask<Void, Integer, Integer> {

		@Override
		protected Integer doInBackground(Void... params) {
			int topMargin = headerLayoutParams.topMargin;
			while (true) {
				topMargin = topMargin + SCROLL_SPEED;
				if (topMargin <= hideHeaderHeight) {
					topMargin = hideHeaderHeight;
					break;
				}
				publishProgress(topMargin);
				sleep(10);
			}
			return topMargin;
		}

		@Override
		protected void onProgressUpdate(Integer... topMargin) {
			headerLayoutParams.topMargin = topMargin[0];
			header.setLayoutParams(headerLayoutParams);
		}

		@Override
		protected void onPostExecute(Integer topMargin) {
			headerLayoutParams.topMargin = topMargin;
			header.setLayoutParams(headerLayoutParams);
			currentStatus = STATUS_REFRESH_FINISHED;
		}
	}

	/**
	 * ʹ��ǰ�߳�˯��ָ���ĺ�������
	 * 
	 * @param time
	 *            ָ����ǰ�߳�˯�߶�ã��Ժ���Ϊ��λ
	 */
	private void sleep(int time) {
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * ����ˢ�µļ�������ʹ������ˢ�µĵط�Ӧ��ע��˼���������ȡˢ�»ص���
	 * 
	 * @author guolin
	 */
	public interface PullToRefreshListener {

		/**
		 * ˢ��ʱ��ȥ�ص��˷������ڷ����ڱ�д�����ˢ���߼���ע��˷����������߳��е��õģ� ����Բ������߳������к�ʱ������
		 */
		void onRefresh();
		
		// Refresh ended
		void onFinish();

	}

}