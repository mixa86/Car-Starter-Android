package com.example.carstarter2;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.telephony.SmsManager;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.yandex.mapkit.Animation;
import com.yandex.mapkit.MapKitFactory;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.map.CameraPosition;
import com.yandex.mapkit.mapview.MapView;

import java.io.InputStream;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity {
	private int DEBUG = 1;
	private String REMOTE_NUMBER = "+79063723857";
	private static String PASS = "fD1DqwUmdShJpPbL+";

	// коды для отправки в ардуино
	private String OUT_CODE_START = "1";
	private String OUT_CODE_STOP = "2";
	private String OUT_CODE_OPEN = "3";
	private String OUT_CODE_CLOSE = "4";
	private String OUT_CODE_GET_INFO = "5";
	private String OUT_CODE_GET_COORDINATES = "6";
	//private String CODE_ENABLE_FAN = "7";

	// коды приходящие из ардуино
	private static String CODE_START_SUCCESS = "1";
	private static String CODE_OPENED_SUCCESS = "2";
	private static String CODE_CLOSED_SUCCESS = "3";
	private static String CODE_STOP_SUCCESS = "4";
	private static String CODE_INFO = "5";
	private static String CODE_COORDINATES = "6";
	String[] debugCommands = {
					"Двигатель запущен",
					"Двигатель остановлен",
					"Двери открыты",
					"Двери закрыты",
					"Получена инфа 1",
					"Получена инфа 2",
					"Получены координаты 1",
					"Получены координаты 2"
	};
	String[] debugCommandsCodes = {
					"1",
					"4",
					"2",
					"3",
					"5:-4_-4_-10_0",
					"5:70_+8_-10_1",
					"6:54.767232_56.029283",
					"6:54.730746_55.949662"
	};

	private static boolean isStarted = false;
	private static final int MY_PERMISSIONS_REQUEST_SEND_SMS = 1;
	private static final int MY_PERMISSIONS_REQUEST_READ_SMS = 2;

	private static Button startButton;
	private static Button stopButton;
	private static Switch opener;
	private static Button debugSmsApply;
	private static Button checkBtn;
	private static Button gpsBtn;

	private Timer timerInterval;
	private Timer timerIntervalVib;
	private int timerCounter = 0;
	private Vibrator vibro;
	private static TextView startingTimerText;
	private static WebView webviewActionView;
	private static TextView loaderMsg;
	private static Spinner debugSms;

	private static TextView engineTemp;
	private static TextView indoorTemp;
	private static TextView outdoorTemp;

	private static boolean confirmation;
	AlertDialog.Builder ad;
	Context context;
	private static MapView mapview;

	protected void onCreate(Bundle savedInstanceState) {
		MapKitFactory.setApiKey("04596534-5fc1-4a3f-a0a1-8891f734502b");
		MapKitFactory.initialize(this);
		setContentView(R.layout.activity_main);
		super.onCreate(savedInstanceState);

		mapview = (MapView) findViewById(R.id.mapview);
		showMapPoint(54.767310, 56.029739);

		if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_SMS},
							MY_PERMISSIONS_REQUEST_READ_SMS);
		}

		startingTimerText = (TextView) findViewById(R.id.startingTimer);
		startingTimerText.setText("--");
		startButton = (Button) findViewById(R.id.startBtn);
		stopButton = (Button) findViewById(R.id.stopBtn);
		stopButton.setEnabled(false);
		opener = (Switch) findViewById(R.id.opener);
		checkBtn = (Button) findViewById(R.id.checkBtn);
		gpsBtn = (Button) findViewById(R.id.gpsBtn);

		vibro = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		webviewActionView = (WebView) findViewById(R.id.webviewcontainer);
		webviewActionView.setWebViewClient(new MyWebViewClient());
		//webviewActionView.getSettings().setJavaScriptEnabled(true);
		loaderMsg = (TextView) findViewById(R.id.loaderMessage);
		debugSms = (Spinner) findViewById(R.id.debugSms);
		debugSmsApply = (Button) findViewById(R.id.debugSmsApply);
		engineTemp = (TextView) findViewById(R.id.engineTemp);
		indoorTemp = (TextView) findViewById(R.id.indoorTemp);
		outdoorTemp = (TextView) findViewById(R.id.outdoorTemp);

		// ОКНО ПОДТВЕРЖДЕНИЯ
		context = MainActivity.this;
		String title = "Подтвердите действие";
		String message = "открытие дверей";
		final String button1String = "Да";
		String button2String = "Нет";
		ad = new AlertDialog.Builder(context);
		ad.setTitle(title);  // заголовок
		ad.setMessage(message); // сообщение
		confirmation = false;

		ad.setNegativeButton(button2String, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int arg1) {
				confirmation = true;
				opener.setChecked(false);
				confirmation = false;
			}
		});
		ad.setCancelable(true);
		ad.setOnCancelListener(new DialogInterface.OnCancelListener() {
			public void onCancel(DialogInterface dialog) {
				confirmation = true;
				opener.setChecked(false);
				confirmation = false;
			}
		});
		// КОНЕЦ ОКНО ПОДТВЕРЖДЕНИЯ

		// ЭМУЛЯЦИЯ ПОЛУЧЕНИЯ СМС ИЗ АРДУИНО
		if (DEBUG == 1) {
			debugSms.setVisibility(View.VISIBLE);
			// Создаем адаптер ArrayAdapter с помощью массива строк и стандартной разметки элемета spinner
			ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, debugCommands);
			// Определяем разметку для использования при выборе элемента
			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			// Применяем адаптер к элементу spinner
			debugSms.setAdapter(adapter);
			final int[] pos = new int[1];
			pos[0] = -1;
			debugSms.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
				@Override
				public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
					pos[0] = position;
				}

				@Override
				public void onNothingSelected(AdapterView<?> parent) {
					pos[0] = -1;
				}
			});
			debugSmsApply.setVisibility(View.VISIBLE);

			debugSmsApply.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (pos[0] == -1)
						return;
					String mess = PASS;
					mess += ":" + debugCommandsCodes[pos[0]];
					onMessage(mess.toString());
				}
			});
		}

		// СТАРТЕР
		startButton.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					vibro.vibrate(200);
					setInfo("00", true);
					timerInterval = new Timer();
					timerInterval.scheduleAtFixedRate(new TimerTask() {
						@Override
						public void run() {
							timerCounter++;
							if (timerCounter < 10) {
								setInfo("0" + timerCounter, true);
							} else {
								setInfo("" + timerCounter, true);
							}
						}
					}, 1000, 1000);

					timerIntervalVib = new Timer();
					timerIntervalVib.scheduleAtFixedRate(new TimerTask() {
						@Override
						public void run() {
							vibro.vibrate(100);
						}
					}, 0, 200);
					return true;
				}
				if (event.getAction() == MotionEvent.ACTION_UP) {
					timerInterval.cancel();
					timerIntervalVib.cancel();
					setButtonsEnabled(false);
					stopButton.setEnabled(false);
					startButton.setEnabled(false);
					showLoader("Соединение с автомобилем\nи попытка запуска");
					smsSend(OUT_CODE_START + ":" + timerCounter);
					timerCounter = 0;
					return true;
				}
				return false;
			}
		});

		// КНОПКА СТОП
		stopButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				vibro.vibrate(100);
				showLoader("Соединение с автомобилем\nвыключение зажигания");
				setButtonsEnabled(false);
				stopButton.setEnabled(false);
				startButton.setEnabled(false);
				smsSend(OUT_CODE_STOP);
				setInfo("--", true);
			}
		});

		// КНОПКА открыть/закрыть двери
		opener.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, final boolean isChecked) {
				if (confirmation == true)
					return;
				vibro.vibrate(100);
				ad.setPositiveButton(button1String, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int arg1) {
						if (isChecked) {
							opener.setText("Закрыть");
							smsSend(OUT_CODE_OPEN);
							showLoader("Соединение с автомобилем\nоткрытие дверей");
							setButtonsEnabled(false);
						}
					}
				});
				if (isChecked == true)
					ad.show();
				if (!isChecked) {
					opener.setText("Открыть");
					smsSend(OUT_CODE_CLOSE);
					showLoader("Соединение с автомобилем\nзакрытие дверей");
					setButtonsEnabled(false);
				}
			}
		});


		// кнопка обновления инфы
		checkBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				vibro.vibrate(100);
				setButtonsEnabled(false);
				showLoader("Соединение с автомобилем\nпроверка");
				smsSend(OUT_CODE_GET_INFO);
			}
		});

		// кнопка обновления инфы
		gpsBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				vibro.vibrate(100);
				setButtonsEnabled(false);
				showLoader("Соединение с автомобилем\nполучение местоположения");
				smsSend(OUT_CODE_GET_COORDINATES);
			}
		});

	}

	/**
	 * для карты
	 */
	@Override
	protected void onStop() {
		super.onStop();
		mapview.onStop();
		MapKitFactory.getInstance().onStop();
	}

	/**
	 * для карты
	 */
	@Override
	protected void onStart() {
		super.onStart();
		mapview.onStart();
		MapKitFactory.getInstance().onStart();
	}

	/**
	 * поставить отметку на карте
	 *
	 * @param latitude
	 * @param longitude
	 */
	static public void showMapPoint(double latitude, double longitude) {
		Point point = new Point(latitude, longitude);
		mapview.getMap().move(
						new CameraPosition(point, 17.0f, 0.0f, 0.0f),
						new Animation(Animation.Type.SMOOTH, 0),
						null);
		mapview.getMap().getMapObjects().clear();
		mapview.getMap().getMapObjects().addPlacemark(point);
	}

	static void setButtonsEnabled(boolean state) {
		startButton.setEnabled(!isStarted);
		stopButton.setEnabled(isStarted);
		opener.setEnabled(state);
		gpsBtn.setEnabled(state);
		checkBtn.setEnabled(state);
	}


	/**
	 * Отправляет команду в ардуино
	 *
	 * @param command - команда без пароля
	 */
	public void smsSend(String command) {
		String messageText = PASS + command;
		if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS},
							MY_PERMISSIONS_REQUEST_SEND_SMS);
		} else {
			if (DEBUG != 1) {
				SmsManager.getDefault().sendTextMessage(REMOTE_NUMBER, null, messageText.toString(), null, null);
			}

		}
	}

	/**
	 * вывести сообщение
	 *
	 * @param message
	 * @param big
	 */
	public static void setInfo(String message, boolean big) {
		if (!big) {
			loaderMsg.setText(message);
			loaderMsg.setVisibility(View.VISIBLE);
		} else {
			startingTimerText.setText(message);
			startingTimerText.setVisibility(View.VISIBLE);
		}
	}

	/**
	 * убрать сообщение
	 */
	public static void unsetInfo() {
		loaderMsg.setText("");
		loaderMsg.setVisibility(View.INVISIBLE);
	}

	/**
	 * @param message
	 */
	public void showLoader(String message) {
		InputStream stream = null;
		try {
			stream = getAssets().open("uond.gif");
		} catch (Exception e) {
			e.printStackTrace();
		}
		webviewActionView.setVisibility(View.VISIBLE);
		if (message != "") {
			setInfo(message, false);
		}
		GifWebView view = new GifWebView(this, stream);
		webviewActionView.addView(view);
	}

	public static void removeLoader() {
		webviewActionView.setVisibility(View.INVISIBLE);
		loaderMsg.setText("");
		loaderMsg.setVisibility(View.INVISIBLE);
	}

	/**
	 * для кота
	 */
	private class MyWebViewClient extends WebViewClient {
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			view.loadUrl(url);
			//view.setScrollY(40);
			return true;
		}
	}

	/**
	 * Обработчик входящих сообщений с ардуино
	 *
	 * @param smsText
	 */
	public static void onMessage(String smsText) {
		String[] parts = smsText.split(":");
		if (parts.length != 2 && parts.length != 3 || !parts[0].equals(PASS))
			return;

		String[] opts = new String[4];
		if (parts.length == 3) {
			opts = parts[2].split("_");
		} else {
			opts[0] = "";
			opts[1] = "";
			opts[2] = "";
			opts[3] = "";
		}

		if (parts[1].equals(CODE_START_SUCCESS)) {
			removeLoader();
			isStarted = true;
			setButtonsEnabled(true);
			setInfo("Двигатель запущен", true);
			if (opts[0] != "") {
				engineTemp.setText(opts[0]);
			}
			if (opts[1] != "") {
				indoorTemp.setText(opts[1]);
			}
			if (opts[2] != "") {
				outdoorTemp.setText(opts[2]);
			}
		}

		if (parts[1].equals(CODE_STOP_SUCCESS)) {
			isStarted = false;
			removeLoader();
			setButtonsEnabled(true);
			setInfo("Зажигание отключено", false);
			setInfo("--", true);
		}

		if (parts[1].equals(CODE_INFO)) {
			removeLoader();
			if (!opts[0].equals("")) {
				engineTemp.setText(opts[0]);
			}
			if (!opts[1].equals("")) {
				indoorTemp.setText(opts[1]);
			}
			if (!opts[2].equals("")) {
				outdoorTemp.setText(opts[2]);
			}
			if (!opts[3].equals("")) {
				String mess = "Двигатель запущен";
				isStarted = true;
				if (opts[3].equals("0")) {
					isStarted = false;
					mess = "Двигатель остановлен";
				}
				setInfo(mess, true);
			}
			setButtonsEnabled(true);
		}

		if (parts[1].equals(CODE_COORDINATES)) {
			removeLoader();
			setButtonsEnabled(true);
			if (opts[0] != "" && opts[1] != "") {
				try {
					double latitude = Double.parseDouble(opts[0]);
					double longitude = Double.parseDouble(opts[1]);
					showMapPoint(latitude, longitude);
				} catch (NumberFormatException e) {
					setInfo("Ошибка", true);
				}
			}
		}

		if (parts[1].equals(CODE_OPENED_SUCCESS)) {
			removeLoader();
			setButtonsEnabled(true);
			setInfo("Двери открыты", true);
		}

		if (parts[1].equals(CODE_CLOSED_SUCCESS)) {
			removeLoader();
			setButtonsEnabled(true);
			setInfo("Двери закрыты", true);
		}

	}
}
