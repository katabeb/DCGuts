package jp.dc.android.dcguts;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.ClipboardManager;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebSettings.RenderPriority;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.CheckBox;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.ads.*;
import com.google.ads.AdRequest.ErrorCode;

public class DCGutsActivity extends Activity implements
		CheckBox.OnCheckedChangeListener,
		DialogInterface.OnDismissListener,
		DialogInterface.OnCancelListener{

	private final String URL_TOP_PAGE = "http://pf.gree.jp/1";
	private final String URL_HOME_PAGE = "http://agkspa.konaminet.jp/agk/web/home.php";
	private final String URL_PLAYER_PAGE = "http://agkspa.konaminet.jp/agk/web/player_detail.php";
	private final String ID1 = "1614160";
	private final String ID2 = "1484042";
	private final int REPEAT_NORMAL_INTERVAL = 3000;
	private final int REPEAT_TURBO_INTERVAL = 1000;
	private final int MAINTE_INTERVAL = 300000;
	private final int ERROR_INTERVAL = 5000;
	private final int TIMEOUT_INTERVAL = 30000;
	private final int MESSAGE_WHAT = 100;
	private final int MESSAGE_TIMEOUT = 101;
	private final int LIST_MAX = 6000;
//	private static final int MENU_ID_URL_COPY = (Menu.FIRST + 1);
	private static final int MENU_ID_SOURCE_COPY = (Menu.FIRST + 2);
	private static final int MENU_ID_URL_JUMP = (Menu.FIRST + 3);
	private static final int MENU_ID_GOTO_TOP = (Menu.FIRST + 4);
	private static final int MENU_ID_GOTO_HOME = (Menu.FIRST + 5);
	private static final int MENU_ID_GET_PLAYER = (Menu.FIRST + 6);
	private static final int MENU_ID_JUMP_PLAYER = (Menu.FIRST + 7);
	private static final int MENU_ID_GOTO_PREF = (Menu.FIRST + 90);
	private static final int MENU_ID_APP_QUIT = (Menu.FIRST + 99);

	/* コントロール */
	private AdView _adView;
	private ClipboardManager _clipboard;
	private ToggleButton _toggleButton;
	private WebView _webview;
	private EditText _edittext;
	private TextView _textview;
	private TextView _textview_point;
	private TextView _textview_page_state;
	private ScrollView _scrollview;
	private HorizontalScrollView _horizontalscrollview;
	private Activity _this;
	private Spinner _spinner;
	private EditText _edttext_url_jump;
	private Dialog _dialog_url_jump;
	private ListView _listview_get_player;
	private Dialog _dialog_get_player;
	private EditText _edttext_player_jump;
	private Dialog _dialog_player_jump;

	/* 内部変数 */
	private List<String> idlist = new ArrayList<String>();
	private int _interval = REPEAT_NORMAL_INTERVAL;
	private int _iCount = 0;
	private int _friends = 0;
	private int _id1 = 0;
	private int _id2 = 0;
	private int _page = 1;
	private int _state = 0;
	private int _state2 = 0;
	private int _state_ex = 0;
	private String _myid = "";

	/* 設定 */
	SharedPreferences _sharedPreferences;
	private List<String> idlist_load = new ArrayList<String>();
	private int _id1_load = 0;
	private int _id2_load = 0;
	public boolean _friend_guts_enable;
	public String _friend_guts_comment;
	public boolean _friend_guts_del;
	public boolean _id_guts_enable;
	public String _id_guts_comment;
	public boolean _id_guts_del;
	public boolean _debug_mode;
	public int _ad_click_date;

	/* リスト */
	private String[] mStrings = { "IDリストで実行", "仲間のみで実行", "IDリスト作成" };

	/* 通知 */
	private NotificationManager _notificationManager;
	private Notification _notification;
	private PendingIntent _contentIntent;
	private String _dellink = "";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		_this = this;

		_notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		_notification = new Notification(
				R.drawable.icon,
				getString(R.string.app_name) + "を起動しています",
				System.currentTimeMillis());
		_notification.flags = Notification.FLAG_ONGOING_EVENT;
		Intent intent = new Intent(this, DCGutsActivity.class);
		//intentの設定
		_contentIntent = PendingIntent.getActivity(this, 0, intent, 0);
		_notification.setLatestEventInfo(
				getApplicationContext(),
				getString(R.string.app_name),
				"待機中",
				_contentIntent);
		_notificationManager.notify(1, _notification);

		_sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

		/* リスト読み込み */
		String sidlist = _sharedPreferences.getString(getString(R.string.idlist_key), "");
		String[] sidlistarray = sidlist.split(",");
		for(int iCnt = 0; iCnt < sidlistarray.length; iCnt++){
			String id = sidlistarray[iCnt];
			if(id != ""){
				if(!idlist_load.contains(id)){
					idlist_load.add(id);
				}
			}
		}
		_friends = _sharedPreferences.getInt(getString(R.string.friends_key), 0);
		_id1_load = _sharedPreferences.getInt(getString(R.string.id1_key), 0);
		_id2_load = _sharedPreferences.getInt(getString(R.string.id2_key), 0);
		/* 設定読み込み */
		doReadPreferences();

		if(_debug_mode){
			_this.setTitle(getString(R.string.app_name) + " " + String.valueOf(_state) + ":" + String.valueOf(idlist_load.size()) + ":" + String.valueOf(_id1_load) + ":" + String.valueOf(_id2_load) + ":" + String.valueOf(_friends));
		}

		_clipboard = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);

		_webview = (WebView)findViewById(R.id.webview);
		_webview.setWebViewClient(new ViewClient() {});
		_webview.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
		_webview.getSettings().setJavaScriptEnabled(true);
		_webview.getSettings().setPluginsEnabled(true);
		_webview.getSettings().setRenderPriority(RenderPriority.LOW);
//		_webview.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
		_webview.getSettings().setAppCacheEnabled(true);
		_webview.getSettings().setAppCacheMaxSize(1048576 * 10);
		_webview.setHorizontalScrollbarOverlay(true);
		_webview.setVerticalScrollbarOverlay(true);
		_webview.addJavascriptInterface(this, "activity");
		_webview.requestFocus(View.FOCUS_DOWN);
		_webview.loadUrl(URL_TOP_PAGE);

		_edittext = (EditText)findViewById(R.id.editText1);
		_edittext.setVisibility(View.GONE);

		_textview_point = (TextView)findViewById(R.id.textview_point);
		_textview_page_state = (TextView)findViewById(R.id.textview_page_state);

		_textview = (TextView)findViewById(R.id.textView1);
		_textview.setVisibility(View.GONE);

		_scrollview = (ScrollView)findViewById(R.id.ScrollView01);
		_scrollview.setVisibility(View.GONE);

		_horizontalscrollview = (HorizontalScrollView)findViewById(R.id.horizontalScrollView1);
		_horizontalscrollview.setVisibility(View.GONE);

		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, mStrings);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		_spinner = (Spinner)findViewById(R.id.spinner1);
		_spinner.setAdapter(adapter);
		_spinner.setSelection(0);

		_toggleButton = (ToggleButton)findViewById(R.id.toggleButton1);
		_toggleButton.setOnCheckedChangeListener(this);
		_toggleButton.setClickable(false);
//		_toggleButton.setChecked(false);

		_edttext_url_jump = new EditText(this);
		_dialog_url_jump = new AlertDialog.Builder(this)
			.setIcon(R.drawable.icon)
			.setTitle("URLを入力")
			.setView(_edttext_url_jump)
			.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					_webview.loadUrl(_edttext_url_jump.getText().toString());
					dialog.dismiss();
				}
			})
			.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					dialog.cancel();
				}
			})
			.create();

		_edttext_player_jump = new EditText(this);
		_edttext_player_jump.setInputType(InputType.TYPE_CLASS_NUMBER);
		_dialog_player_jump = new AlertDialog.Builder(this)
			.setIcon(R.drawable.icon)
			.setTitle("インデックスを入力")
			.setView(_edttext_player_jump)
			.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					String sid = _edttext_player_jump.getText().toString();
					if(sid.matches("^[0-9]{1,4}$")){
						int idx = Integer.parseInt(sid);
						if(idx >= 0 && idx < idlist_load.size()){
							_webview.loadUrl(URL_PLAYER_PAGE + "?user_id=" + idlist_load.get(idx));
						}
					}
					dialog.dismiss();
				}
			})
			.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					dialog.cancel();
				}
			})
			.create();

		_listview_get_player = new ListView(this);
		_listview_get_player.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> items, View view, int position, long id) {
				_dialog_get_player.dismiss();
				String item[] = items.getItemAtPosition(position).toString().split("\n");
				if(item.length > 0){
					_webview.loadUrl(URL_PLAYER_PAGE + "?user_id=" + item[0]);
				}
			}
		});
		_dialog_get_player = new AlertDialog.Builder(this)
			.setIcon(R.drawable.icon)
			.setTitle("表示する相手を選択")
			.setView(_listview_get_player)
			.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					dialog.cancel();
				}
			})
			.create();

		_adView = (AdView)this.findViewById(R.id.adView);
		AdRequest adr = new AdRequest();
		_adView.loadAd(adr);
		_adView.setAdListener(AdEventListener);
	}

	/* 設定読み込み */
	private void doReadPreferences(){
		_friend_guts_enable = _sharedPreferences.getBoolean(getString(R.string.friend_guts_enable_key), true);
		_friend_guts_comment = _sharedPreferences.getString(getString(R.string.friend_guts_comment_key), "");
		_friend_guts_del = _sharedPreferences.getBoolean(getString(R.string.friend_guts_del_key), true);
		_id_guts_enable = _sharedPreferences.getBoolean(getString(R.string.id_guts_enable_key), true);
		_id_guts_comment = _sharedPreferences.getString(getString(R.string.id_guts_comment_key), "");
		_id_guts_del = _sharedPreferences.getBoolean(getString(R.string.id_guts_del_key), true);
		_debug_mode = _sharedPreferences.getBoolean(getString(R.string.debug_mode_key), false);
		_ad_click_date = _sharedPreferences.getInt(getString(R.string.ad_click_date_key), 0);
		/* 設定保存 */
		doWritePreferences();
	}

	/* 設定保存 */
	private void doWritePreferences(){
		Editor ed = _sharedPreferences.edit();
		ed.putBoolean(getString(R.string.friend_guts_enable_key), _friend_guts_enable);
		ed.putString(getString(R.string.friend_guts_comment_key), _friend_guts_comment);
		ed.putBoolean(getString(R.string.friend_guts_del_key), _friend_guts_del);
		ed.putBoolean(getString(R.string.id_guts_enable_key), _id_guts_enable);
		ed.putString(getString(R.string.id_guts_comment_key), _id_guts_comment);
		ed.putBoolean(getString(R.string.id_guts_del_key), _id_guts_del);
		ed.putBoolean(getString(R.string.debug_mode_key), _debug_mode);
		ed.putInt(getString(R.string.ad_click_date_key), _ad_click_date);
		ed.commit();
	}

	/* 広告表示切替 */
	public void doAdShow() {
		String title = getString(R.string.app_name);
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.HOUR, -3);
		final int year = calendar.get(Calendar.YEAR);
		final int month = calendar.get(Calendar.MONTH);
		final int day = calendar.get(Calendar.DAY_OF_MONTH);
		final int now = year * 10000 + month * 100 + day;

		if(_myid.equals(ID1) || _myid.equals(ID2)){
			title += "(VIP)";
		}

		if(_ad_click_date == now){
			_adView.setVisibility(View.GONE);
			_interval = REPEAT_TURBO_INTERVAL;
			title += " - 高速モード";
		}
		else{
			_adView.setVisibility(View.VISIBLE);
			_interval = REPEAT_NORMAL_INTERVAL;
		}
		_this.setTitle(title);
	}

	/** 広告の状態によって処理を行う */
	private AdListener AdEventListener = new AdListener() {
		/** 広告が表示されたら呼ばれる */
		public void onReceiveAd(Ad ad) {
//			Toast.makeText(_this, "onReceiveAd()", Toast.LENGTH_SHORT).show();
		}
		/** 広告がクリックされたら呼ばれる */
		public void onPresentScreen(Ad ad) {
//			Toast.makeText(_this, "onPresentScreen()", Toast.LENGTH_SHORT).show();
			/* クリック日時を保存 */
			Calendar calendar = Calendar.getInstance();
			calendar.add(Calendar.HOUR, -3);
			final int year = calendar.get(Calendar.YEAR);
			final int month = calendar.get(Calendar.MONTH);
			final int day = calendar.get(Calendar.DAY_OF_MONTH);
			_ad_click_date = year * 10000 + month * 100 + day;
			/* 設定更新 */
			doWritePreferences();
			/* 広告表示切替 */
			doAdShow();
		}
		/** 表示された広告から別のアクティビティが開かれた時に呼ばれる･･･らしい */
		public void onLeaveApplication(Ad ad) {
//			Toast.makeText(_this, "onLeaveApplication()", Toast.LENGTH_SHORT).show();
		}
		/** 広告が表示出来なかったら呼ばれる */
		public void onFailedToReceiveAd(Ad ad, ErrorCode errorcode) {
//			Toast.makeText(_this, "onFailedToReceiveAd()", Toast.LENGTH_SHORT).show();
		}
		/** 広告表示から戻ったときに呼ばれる */
		public void onDismissScreen(Ad ad) {
//			Toast.makeText(_this, "onDismissScreen()", Toast.LENGTH_SHORT).show();
		}
	};

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		if (event.getAction() == KeyEvent.ACTION_DOWN) {
			if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
				if (_toggleButton.isChecked()) {
					Toast.makeText(this, "実行を停止しました。", Toast.LENGTH_SHORT).show();
					_toggleButton.setChecked(false);
					return true;
				}
				else{
					if(_webview.canGoBack()){
						_webview.goBack();
						return true;
					}
				}
			}
		}
		return super.dispatchKeyEvent(event);
	}

	@Override
	public void onResume() {
		super.onResume();
		/* 設定読み込み */
		doReadPreferences();
		/* 広告表示切替 */
		doAdShow();
		/* CPU節約のため */
		if(!_toggleButton.isChecked()){
			_webview.setVisibility(View.VISIBLE);
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		/* CPU節約のため */
		if(!_toggleButton.isChecked()){
			_webview.setVisibility(View.GONE);
		}
	}

	@Override
	public void onDestroy() {
//		Toast.makeText(this, "onDestroy()", Toast.LENGTH_SHORT).show();
		super.onDestroy();
		NotificationManager mManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
		mManager.cancel(1);
		_webview.loadUrl("about:blank");
		finish();
	}

	// オプションメニューが最初に呼び出される時に1度だけ呼び出されます
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// メニューアイテムを追加します
		menu.add(Menu.NONE, MENU_ID_GOTO_TOP, Menu.NONE, "トップへ");
		menu.add(Menu.NONE, MENU_ID_GOTO_HOME, Menu.NONE, "ホームへ");
		menu.add(Menu.NONE, MENU_ID_GET_PLAYER, Menu.NONE, "相手抽出");
//		menu.add(Menu.NONE, MENU_ID_URL_COPY, Menu.NONE, "URLコピー");
		menu.add(Menu.NONE, MENU_ID_SOURCE_COPY, Menu.NONE, "ソースコピー");
		menu.add(Menu.NONE, MENU_ID_URL_JUMP, Menu.NONE, "URL移動");
		menu.add(Menu.NONE, MENU_ID_JUMP_PLAYER, Menu.NONE, "リストジャンプ");
		menu.add(Menu.NONE, MENU_ID_GOTO_PREF, Menu.NONE, "設定");
		menu.add(Menu.NONE, MENU_ID_APP_QUIT, Menu.NONE, "終了");
		return super.onCreateOptionsMenu(menu);
	}

	// オプションメニューが表示される度に呼び出されます
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.findItem(MENU_ID_GOTO_TOP).setVisible(!_toggleButton.isChecked());
		menu.findItem(MENU_ID_GOTO_HOME).setVisible(!_toggleButton.isChecked());
		menu.findItem(MENU_ID_GET_PLAYER).setVisible(!_toggleButton.isChecked());
//		menu.findItem(MENU_ID_URL_COPY).setVisible(!_toggleButton.isChecked());
		menu.findItem(MENU_ID_SOURCE_COPY).setVisible(!_toggleButton.isChecked() && _debug_mode);
		menu.findItem(MENU_ID_URL_JUMP).setVisible(!_toggleButton.isChecked() && _debug_mode);
		menu.findItem(MENU_ID_JUMP_PLAYER).setVisible(!_toggleButton.isChecked() && _debug_mode);
		menu.findItem(MENU_ID_GOTO_PREF).setVisible(!_toggleButton.isChecked());
		menu.findItem(MENU_ID_APP_QUIT).setVisible(!_toggleButton.isChecked());
		return super.onPrepareOptionsMenu(menu);
	}

	// オプションメニューアイテムが選択された時に呼び出されます
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		boolean ret = true;
		switch (item.getItemId()) {
			case MENU_ID_GOTO_TOP:
				_webview.loadUrl(URL_TOP_PAGE);
				break;
			case MENU_ID_GOTO_HOME:
				_webview.loadUrl(URL_HOME_PAGE);
				break;
			case MENU_ID_GET_PLAYER:
				final ArrayList<String> rows = doPlayerGet();

				if(rows.size() > 0){
					ArrayAdapter<String> adp = new ArrayAdapter<String>(this, R.layout.listview, rows);
					_listview_get_player.setAdapter(adp);
					_dialog_get_player.setOnCancelListener(this);
					_dialog_get_player.setOnDismissListener(this);
					_dialog_get_player.show();
				}
				else{
					if(_webview.getUrl().equals(URL_TOP_PAGE)){
						Toast.makeText(_this, "一度ホームに移動してください", Toast.LENGTH_SHORT).show();
					}
					else{
						Toast.makeText(_this, "相手がみつかりません", Toast.LENGTH_SHORT).show();
					}
				}
				break;
			case MENU_ID_SOURCE_COPY:
				_clipboard.setText(_textview.getText().toString());
				ret = true;
				break;
			case MENU_ID_URL_JUMP:
				_edttext_url_jump.setText(_webview.getUrl());
				_dialog_url_jump.setOnCancelListener(this);
				_dialog_url_jump.setOnDismissListener(this);
				_dialog_url_jump.show();
				break;
			case MENU_ID_JUMP_PLAYER:
				_edttext_player_jump.setText(String.valueOf(_id1_load));
				_dialog_player_jump.show();
				break;
			case MENU_ID_GOTO_PREF:
				Intent intent = new Intent(DCGutsActivity.this,DCGutsPrefActivity.class);
				startActivity(intent);
				break;
			case MENU_ID_APP_QUIT:
				onDestroy();
				break;
			default:
				ret = super.onOptionsItemSelected(item);
				break;
		}
		return ret;
	}

	private Handler handler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			switch(msg.what){
			case MESSAGE_WHAT:
				doSomething();
				break;
			case MESSAGE_TIMEOUT:
				_webview.loadUrl(URL_TOP_PAGE);
				break;
			}
		}
	};

	public void viewSource(final String src) {
		handler.post(new Runnable() {
			public void run() {
				String url = _edittext.getText().toString();

				_textview.setText(src);
				if (_toggleButton.isChecked()) {
					_webview.clearHistory();
					if (url.matches("^http://agkspa\\.konaminet\\.jp/agk/web/sessionerror\\.php\\?errorcode=90001.*") ||
						url.matches("^http://agkspa\\.konaminet\\.jp/agk/web/error\\.php\\?errno=90005\\&var=0.*")){
						//セッションタイムアウト
						//ブラウザバックエラー
						_state_ex = 1;
						Message message = new Message();
						message.what = MESSAGE_WHAT;
						handler.sendMessageDelayed(message, ERROR_INTERVAL);
					}
					else if (url.matches("^http://agkspa\\.konaminet\\.jp/agk/web/mainte\\.php.*") ){
						//メンテナンス中
						_textview_point.setText("");
						_textview_page_state.setText("メンテナンス中...(５分間隔で確認)");
						switch(_state){
						case 3:
						case 4:
						case 5:
						case 6:
							_state = 3;
							break;
						default:
							break;
						}
						Message message = new Message();
						message.what = MESSAGE_WHAT;
						handler.sendMessageDelayed(message, MAINTE_INTERVAL);
					}
					else{
						boolean bNoMsg = false;
						boolean b_next_player = false;
						int interval = _interval;

						switch(_state){
						case 0:
						case 1:
						case 2:
						case 5:
						case 6:
							interval = 10;
							break;
						default:
							interval = _interval;
							break;
						}

//						if (url.matches("^http://agkspa\\.konaminet\\.jp/agk/web/friend/friend_search\\.php.*")){
//							doIDGet();
//						}
						if (url.matches("^http://agkspa\\.konaminet\\.jp/agk/web/home\\.php.*") ){
							//ホーム
							if(_state == 0){
								_myid = getMyID();
//								Toast.makeText(_this, "MyID:" + _myid, Toast.LENGTH_SHORT).show();
								switch(_spinner.getSelectedItemPosition()){
								case 0:
									_state = 3;
									_state2 = 0;
									break;
								case 1:
									_state = 1;
									_page = 1;
									break;
								case 2:
									_state = 1;
									_page = 1;
									break;
								}
							}
						}
						else if (	url.matches("^http://agkspa\\.konaminet\\.jp/agk/web/friend/friend_list\\.php.*") ||
									url.matches("^http://agkspa\\.konaminet\\.jp/agk/web/greet/greet_list\\.php.*")){
							//仲間一覧・ガッツ履歴
							if(_state == 1 || _state == 2){
								int iGetCnt = doIDGet();
								if(iGetCnt == 0 || (_state == 2 && _page == 2)){
									if (_state == 2){
										_id2++;
									}
									else{
										_friends = idlist.size();
									}
									if(_spinner.getSelectedItemPosition() == 2){
										if (idlist.size() > LIST_MAX){
											_toggleButton.setChecked(false);
											bNoMsg = true;
										}
										else{
											if (idlist.size() > _id1){
												_state = 2;
											}
										}
									}
									else{
										if (idlist.size() > _id1){
											_state = 3;
										}
									}
									_page = 1;
								}
								else{
									_page++;
								}
							}
						}
						else if (	url.matches("^http://agkspa\\.konaminet\\.jp/agk/web/greet/greet_do\\.php.*") ||
									url.matches("^http://agkspa\\.konaminet\\.jp/agk/web/greet/greet_do2\\.php.*")){
							//ガッツ(入力)ページ
							if(_state == 3){
								String point = doPointGet();
								if(!point.equals("")){
									_textview_point.setText("友情pt:" + point + " ");
								}
								else{
									_textview_point.setText("");
								}
								switch(_spinner.getSelectedItemPosition()){
								case 0:
									if(_id_guts_enable){
										/* 仮送信 */
										_state = 4;
									}
									else{
										b_next_player = true;
									}
									break;
								case 1:
									if(_friend_guts_enable){
										/* 仮送信 */
										_state = 4;
									}
									else{
										b_next_player = true;
									}
									break;
								case 2:
									break;
								}
							}
						}
						else if (url.matches("^http://agkspa\\.konaminet\\.jp/agk/web/greet/greeting_up\\.php.*")){
							//ガッツ(送信確認)ページ
							if(_state == 4){
								String point = doPointGet();
								if(!point.equals("")){
									_textview_point.setText("友情pt:" + point + " ");
								}
								else{
									_textview_point.setText("");
								}
								/* 本送信 */
								_state = 5;
							}
						}
						else if (url.matches("^http://agkspa\\.konaminet\\.jp/agk/web/player_detail\\.php.*")){
							//プレイヤーページ
							if(_state == 5 || _state == 6){
								/* 削除 */
								if(	(_spinner.getSelectedItemPosition() == 0 && _id_guts_del) ||
									(_spinner.getSelectedItemPosition() == 1 && _friend_guts_del)){
									String dellink = doDeleteLinkGet();
									dellink = dellink.replace("&amp;", "&");
									_dellink = dellink;
									if(dellink.equals("")){
										b_next_player = true;
									}
									else{
										_state = 6;
									}
								}
								else{
									b_next_player = true;
								}
							}
						}
						else if (url.matches("^http://agkspa\\.konaminet\\.jp/agk/web/error\\.php\\?errno=1001\\&var=0.*") ){
							//アクセスできません
							b_next_player = true;
						}
						else if (url.matches("^http://agkspa\\.konaminet\\.jp/agk/web/error\\.php\\?errno=3001\\&var=3.*") ){
							//制限越え
							Calendar calendar = Calendar.getInstance();
							final int year = calendar.get(Calendar.YEAR);
							final int month = calendar.get(Calendar.MONTH);
							final int day = calendar.get(Calendar.DAY_OF_MONTH);
							final int hour = calendar.get(Calendar.HOUR);
							long time_from;
							long time_to;

							time_from = calendar.getTimeInMillis();
							if(hour > 3){
								calendar.set(year, month, day, 3, 1, 0);
								calendar.add(Calendar.DAY_OF_MONTH, 1);
							}
							else{
								calendar.set(year, month, day, 3, 1, 0);
							}
							time_to = calendar.getTimeInMillis();
							interval = (int)(time_to - time_from);

//							Toast.makeText(_this, "一日のガッツ数制限に達しました。\n３時まで待機します。", Toast.LENGTH_SHORT).show();
							_textview_point.setText("");
							_textview_page_state.setText("ガッツ数制限のため３時まで待機...");
							_state = 3;
						}
						else{
							_state = 3;
						}
						if(b_next_player){
							/* 次の相手へ */
							_state = 3;
							if(_state2 == 0){
								_id1++;
							}
							_iCount++;
							if (_iCount == 1000){
								_iCount = 0;
							}
							if (idlist.size() <= _id1){
								if(_spinner.getSelectedItemPosition() == 1){
									_toggleButton.setChecked(false);
									Toast.makeText(_this, "仲間全員にガッツしました。", Toast.LENGTH_SHORT).show();
									bNoMsg = true;
								}
								else{
									if (idlist.size() <= LIST_MAX){
										_state = 2;
										_page = 1;
									}
									else{
										_id1 = _friends;
									}
								}
							}
							switch(_spinner.getSelectedItemPosition()){
							case 0:
								/* 設定の保存 */
								Editor ed = _sharedPreferences.edit();
								_id1_load = _id1;
								_id2_load = _id2;
								ed.putInt(getString(R.string.id1_key), _id1);
								ed.putInt(getString(R.string.id2_key), _id2);
								ed.commit();
								break;
							case 1:
								break;
							case 2:
								break;
							}
						}
						if(_debug_mode){
							_this.setTitle(getString(R.string.app_name) + " " + String.valueOf(_state) + ":" + String.valueOf(idlist.size()) + ":" + String.valueOf(_id1) + ":" + String.valueOf(_id2) + ":" + String.valueOf(_friends));
						}
						if(!bNoMsg){
							Message message = new Message();
							message.what = MESSAGE_WHAT;
							handler.sendMessageDelayed(message, interval);
						}
					}
				}
				else{
					/* ガベージコレクション */
					System.gc();
				}
				_toggleButton.setClickable(true);
			}
		});
	}

	public class ViewClient extends WebViewClient {
		@Override
		public void onPageStarted(WebView view, String url, Bitmap favicon){
			_textview_page_state.setText("ページ読込中...");
			_edittext.setText(url);
			Message message = new Message();
			message.what = MESSAGE_TIMEOUT;
			handler.sendMessageDelayed(message, TIMEOUT_INTERVAL);
		}
		@Override
		public void onPageFinished(WebView view , String url){
			_textview_page_state.setText("");
			handler.removeMessages(MESSAGE_TIMEOUT);
			_textview.setText("");
			view.loadUrl("javascript:window.activity.viewSource(document.documentElement.outerHTML);");
		}
	}

	public void onCheckedChanged(CompoundButton _tbutton,boolean _checked){
		_webview.setClickable(!_checked);
		_spinner.setClickable(!_checked);
		if(_checked){
			_webview.setVisibility(View.GONE);
		}
		else{
			_webview.setVisibility(View.VISIBLE);
		}
		/* ガベージコレクション */
		System.gc();

		if(_checked){
			if(_spinner.getSelectedItemPosition() == 0 && idlist_load.size() <= LIST_MAX){
				Toast.makeText(_this, "IDリストを作成してから実行してください。", Toast.LENGTH_SHORT).show();
				_toggleButton.setChecked(false);
				onCheckedChanged(_tbutton,false);
			}
			else{
				_webview.getSettings().setLoadsImagesAutomatically(false);
				_webview.getSettings().setBlockNetworkImage(true);

				switch(_spinner.getSelectedItemPosition()){
				case 0:
					idlist.clear();
					for(int iCnt = 0; iCnt < idlist_load.size(); iCnt++){
						idlist.add(idlist_load.get(iCnt));
					}
					if(_id1_load < _friends){
						_id1 = _friends;
					}
					else{
						_id1 = _id1_load;
					}
					_id2 = _id2_load;
					_state = 0;
					_state2 = 0;
					break;
				case 1:
					idlist.clear();
					_id1 = 0;
					_id2 = 0;
					_state = 0;
					_state2 = 0;
					break;
				case 2:
					idlist_load.clear();
					_id1_load = 0;
					_id2_load = 0;
					idlist.clear();
					_friends = 0;
					_id1 = 0;
					_id2 = 0;
					_state = 0;
					_state2 = 0;
					break;
				}

				Message message = new Message();
				message.what = MESSAGE_WHAT;
				handler.sendMessageDelayed(message, _interval);

				_notification.setLatestEventInfo(
						getApplicationContext(),
						getString(R.string.app_name),
						"実行中",
						_contentIntent);
				_notificationManager.notify(1, _notification);
			}
		}
		else{
			handler.removeMessages(MESSAGE_WHAT);
			handler.removeMessages(MESSAGE_TIMEOUT);

			_textview_point.setText("");
			_textview_page_state.setText("");

			_webview.getSettings().setLoadsImagesAutomatically(true);
			_webview.getSettings().setBlockNetworkImage(false);

			_notification.setLatestEventInfo(
					getApplicationContext(),
					getString(R.string.app_name),
					"待機中",
					_contentIntent);
			_notificationManager.notify(1, _notification);

			Editor ed = _sharedPreferences.edit();
			switch(_spinner.getSelectedItemPosition()){
			case 0:
				/* 設定の保存 */
				_id1_load = _id1;
				_id2_load = _id2;
				ed.putInt(getString(R.string.id1_key), _id1);
				ed.putInt(getString(R.string.id2_key), _id2);
				Toast.makeText(_this, "現在の状態を保存しました。", Toast.LENGTH_SHORT).show();
				break;
			case 1:
				idlist.clear();
				_id1 = 0;
				_id2 = 0;
				_state = 0;
				_state2 = 0;
				break;
			case 2:
				if(idlist.size() > LIST_MAX){
					/* 設定の保存 */
					String sidlist = "";
					idlist_load.clear();
					for(int iCnt = 0; iCnt < idlist.size(); iCnt++){
						idlist_load.add(idlist.get(iCnt));
						sidlist = sidlist + idlist.get(iCnt) + ",";
					}
					if(sidlist.length() > 0){
						sidlist = sidlist.substring(0, sidlist.length()-1);
					}
					_id1_load = _id1;
					_id2_load = _id2;
					ed.putString(getString(R.string.idlist_key), sidlist);
					ed.putInt(getString(R.string.friends_key), _friends);
					ed.putInt(getString(R.string.id1_key), _id1);
					ed.putInt(getString(R.string.id2_key), _id2);
					Toast.makeText(_this, "作成したリストを保存しました。", Toast.LENGTH_SHORT).show();
				}
				else{
					Toast.makeText(_this, "リスト作成を中止しました。\n再度作成を行ってください。", Toast.LENGTH_SHORT).show();
				}
				break;
			}
			ed.commit();
		}
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		// ここに、ダイアログの表示前に必要な処理を記述する。
		super.onPrepareDialog(id, dialog);
	}

	public void onDismiss(DialogInterface dialog) {
	}

	public void onCancel(DialogInterface dialog) {
	}

	private String doPointGet(){
		String point = "";

		/* ガッツから抽出 */
		String target = _textview.getText().toString().replaceAll("</a>", "</a>\n");
		Pattern pattern = Pattern.compile("<span style=\"color:#fabf00;\">友情pt</span>:[0-9]+ → <span style=\"color:#00ff00;\">([0-9]+)</span>");
		Matcher matcher = pattern.matcher(target);
		while (matcher.find()) {
			point = matcher.group(1);
			break;
		}
		return point;
	}

	private String doDeleteLinkGet(){
		String deletelink = "";

		/* プレイヤー一覧から抽出 */
		String target = _textview.getText().toString().replaceAll("</a>", "</a>\n");
		Pattern pattern = Pattern.compile("(http://.*/greet_deldo\\.php.*[^\\\"]+)\\\">");
		Matcher matcher = pattern.matcher(target);
		while (matcher.find()) {
			deletelink = matcher.group(1);
			break;
		}
		return deletelink;
	}

	private ArrayList<String> doPlayerGet(){
		final ArrayList<String> rows = new ArrayList<String>();

		/* プレイヤー一覧から抽出 */
		String target = _textview.getText().toString().replaceAll("</a>", "</a>\n");
		Pattern pattern = Pattern.compile("player_detail\\.php\\?user_id=([0-9]+).*>([^<]+)</a>");
		Matcher matcher = pattern.matcher(target);
		while (matcher.find()) {
			rows.add(matcher.group(1) + "\n" + matcher.group(2));
		}
		/* バトル相手一覧から抽出 */
		Pattern pattern2 = Pattern.compile("battle\\.php\\?rival_id=([0-9]+).*>([^<]+)</a>");
		Matcher matcher2 = pattern2.matcher(target);
		while (matcher2.find()) {
			rows.add(matcher2.group(1) + "\n" + matcher2.group(2));
		}
		/* バトルページから抽出 */
		Pattern pattern3 = Pattern.compile("battle\\.php\\?rival_id=([0-9]+)");
		Matcher matcher3 = pattern3.matcher(_webview.getUrl());
		while (matcher3.find()) {
			Pattern pattern4 = Pattern.compile(">相手</span>([^<]+)<br>");
			Matcher matcher4 = pattern4.matcher(target);
			while (matcher4.find()) {
				rows.add(matcher3.group(1) + "\n" + matcher4.group(1));
			}
		}
		return rows;
	}

	private int doIDGet(){
		int iMatch = 0;
		String target = _textview.getText().toString();
		Pattern pattern = Pattern.compile("player_detail\\.php\\?user_id=([0-9]+)");
		Matcher matcher = pattern.matcher(target);
		while (matcher.find()) {
			if(_myid != matcher.group(1)){
				if(!idlist.contains(matcher.group(1))){
					idlist.add(matcher.group(1));
					iMatch++;
				}
			}
		}
		return iMatch;
	}

	private String getMyID(){
		String rtn = "";
		String target = _textview.getText().toString();
		Pattern pattern = Pattern.compile("greet/greet_list\\.php\\?user_id=([0-9]+)");
		Matcher matcher = pattern.matcher(target);
		while (matcher.find()) {
			rtn = matcher.group(1);
		}
		return rtn;
	}

	private void doSomething() {
		String id = "";
		String url = "";

		if(_state_ex != 0){
			switch(_state_ex){
			case 1:
				_webview.loadUrl(URL_TOP_PAGE);
				break;
			}
			_state_ex = 0;
			return;
		}

		switch (_state){
		case 0:
			url = URL_HOME_PAGE;
			_webview.loadUrl(url);
			break;
		case 1:
			/* 仲間一覧 */
			url = "http://agkspa.konaminet.jp/agk/web/friend/friend_list.php?page=" + String.valueOf(_page);
			_webview.loadUrl(url);
			break;
		case 2:
			/* ガッツ履歴 */
			switch(_spinner.getSelectedItemPosition()){
			case 0:
				break;
			case 1:
				if(_id2 >= idlist.size()){
					_id2 = _friends;
				}
				break;
			case 2:
				break;
			}
			if(_id2 < idlist.size()){
				id = idlist.get(_id2);
				url = "http://agkspa.konaminet.jp/agk/web/greet/greet_list.php?user_id=" + id + "&page=" + String.valueOf(_page);
				_webview.loadUrl(url);
			}
			else{
				Toast.makeText(_this, "IDリストの異常を検知。\n処理を停止します。", Toast.LENGTH_SHORT).show();
				_toggleButton.setChecked(false);
			}
			break;
		case 3:
			if(_iCount % 100 == 0){
				/* ガベージコレクション */
				System.gc();
			}

			/* ガッツする */
			if(_myid.equals(ID1) || _myid.equals(ID2)){
				_state2 = 0;
			}
			else{
				if (_iCount == 128){
					_state2 = 1;
					id = ID1;
				}
				else if (_iCount == 256){
					_state2 = 1;
					id = ID2;
				}
				else{
					_state2 = 0;
				}
			}
			if(_state2 == 0){
				switch(_spinner.getSelectedItemPosition()){
				case 0:
					if(_id1 >= idlist.size()){
						_id1 = _friends;
					}
					break;
				case 1:
					break;
				case 2:
					if(_id1 >= idlist.size()){
						_id1 = 0;
					}
					break;
				}
			}
			if(_id1 < idlist.size()){
				id = idlist.get(_id1);
				url = "http://agkspa.konaminet.jp/agk/web/greet/greet_do.php?target_id=" + id + "&fl=0&is_friend=1";
				_webview.loadUrl(url);
			}
			else{
				Toast.makeText(_this, "IDリストの異常を検知。\n処理を停止します。", Toast.LENGTH_SHORT).show();
				_toggleButton.setChecked(false);
			}
			break;
		case 4:
			/* ガッツ仮送信 */
			String comment;
			if(_spinner.getSelectedItemPosition() == 0){
				comment = _id_guts_comment;
			}
			else{
				comment = _friend_guts_comment;
			}
			if(comment.equals("")){
				comment = " ";
			}
			_webview.loadUrl("javascript:(function() {var a = 0;var b = 0;b = document.getElementsByName('comment');for (a = 0; a < b.length; a++) {b[a].value='" + comment + "'};document.do_guts.submit();})()");
			break;
		case 5:
			/* ガッツ本送信 */
			_webview.loadUrl("javascript:document.do_guts.fusionButton2.click();");
			break;
		case 6:
			/* ガッツ削除 */
			if(!_dellink.equals("")){
				_webview.loadUrl(_dellink);
			}
			break;
		default:
			break;
		}
	}
}