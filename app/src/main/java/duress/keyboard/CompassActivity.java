package duress.keyboard;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class CompassActivity extends Activity implements SensorEventListener {

    private CompassView compassView;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;

    private float[] gravityValues = new float[3];
    private float[] geomagneticValues = new float[3];
    private float azimuth = 0f;

    private boolean isRussian;
    private FrameLayout root;
    private boolean isRecentScreen = false;
    private int navBarHeight = 120;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Locale locale = Locale.getDefault();
        isRussian = locale != null && "ru".equalsIgnoreCase(locale.getLanguage());

        root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        setContentView(root);

        navBarHeight = (int) (56 * getResources().getDisplayMetrics().density);

        showCompassScreen();

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
    }

    @Override
    protected void onResume() {
        super.onResume();
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        );
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onBackPressed() {
        if (isRecentScreen) {
            showCompassScreen();
        } else {
            super.onBackPressed();
        }
    }

    private void showCompassScreen() {
        isRecentScreen = false;
        root.removeAllViews();

        compassView = new CompassView(this);
        FrameLayout.LayoutParams compassParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        );
        root.addView(compassView, compassParams);

        addNavigationBar();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            gravityValues = event.values.clone();
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            geomagneticValues = event.values.clone();
        }

        if (gravityValues != null && geomagneticValues != null) {
            float[] rotationMatrix = new float[9];
            float[] inclinationMatrix = new float[9];

            boolean success = SensorManager.getRotationMatrix(
                rotationMatrix, 
                inclinationMatrix, 
                gravityValues, 
                geomagneticValues
            );

            if (success) {
                float[] orientation = new float[3];
                SensorManager.getOrientation(rotationMatrix, orientation);
                azimuth = (float) Math.toDegrees(orientation[0]);
                azimuth = (azimuth + 360) % 360;

                if (compassView != null) {
                    compassView.updateAzimuth(azimuth);
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private void showRecentAppsScreen() {
        isRecentScreen = true;
        root.removeAllViews();

        FrameLayout recentContainer = new FrameLayout(this);
        recentContainer.setBackgroundColor(Color.parseColor("#1A1A1A"));
        FrameLayout.LayoutParams recentParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        );
        root.addView(recentContainer, recentParams);

        TextView noRecentText = new TextView(this);
        noRecentText.setText(isRussian ? "Нет последних приложений" : "No recent applications");
        noRecentText.setTextSize(20);
        noRecentText.setTextColor(Color.parseColor("#CCCCCC"));
        noRecentText.setGravity(Gravity.CENTER);

        FrameLayout.LayoutParams textParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        );
        textParams.gravity = Gravity.CENTER;

        recentContainer.addView(noRecentText, textParams);

        addNavigationBar();
    }

    private void addNavigationBar() {
        final LinearLayout navBar = new LinearLayout(this);
        navBar.setOrientation(LinearLayout.HORIZONTAL);
        navBar.setBackgroundColor(Color.parseColor("#E61A1A1A"));

        final FrameLayout.LayoutParams navParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            navBarHeight
        );
        navParams.gravity = Gravity.BOTTOM;

        root.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
				@Override
				public void onGlobalLayout() {
					root.getViewTreeObserver().removeOnGlobalLayoutListener(this);
					navParams.gravity = Gravity.BOTTOM;
					navBar.setLayoutParams(navParams);
				}
			});

        root.addView(navBar, navParams);

        Button backButton = createNavButton("<");
        backButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					try {
						Intent intent = new Intent(CompassActivity.this, 
												   Class.forName("duress.keyboard.LauncherActivity"));
						startActivity(intent);
					} catch (ClassNotFoundException e) {
						Toast.makeText(CompassActivity.this, 
									   isRussian ? "LauncherActivity не найден" : "LauncherActivity not found", 
									   Toast.LENGTH_SHORT).show();
					}
				}
			});

        Button homeButton = createNavButton("□");
        homeButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					try {
						Intent intent = new Intent(CompassActivity.this, 
												   Class.forName("duress.keyboard.LauncherActivity"));
						startActivity(intent);
					} catch (ClassNotFoundException e) {
						Toast.makeText(CompassActivity.this, 
									   isRussian ? "LauncherActivity не найден" : "LauncherActivity not found", 
									   Toast.LENGTH_SHORT).show();
					}
				}
			});

        Button recentButton = createNavButton("|||");
        recentButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (isRecentScreen) {
						showCompassScreen();
					} else {
						showRecentAppsScreen();
					}
				}
			});

        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.MATCH_PARENT
        );
        buttonParams.weight = 1;

        navBar.addView(backButton, buttonParams);
        navBar.addView(homeButton, buttonParams);
        navBar.addView(recentButton, buttonParams);
    }

    private Button createNavButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(24);
        button.setTextColor(Color.WHITE);
        button.setBackgroundColor(Color.TRANSPARENT);
        button.setAllCaps(false);
        button.setPadding(0, 0, 0, 0);
        return button;
    }

    class CompassView extends View {
        private Paint backgroundPaint;
        private Paint northArrowPaint;
        private Paint southArrowPaint;
        private Paint eastArrowPaint;
        private Paint westArrowPaint;
        private Paint intermediateArrowPaint;
        private Paint mainTextPaint;
        private Paint tickPaint;
        private Paint centerPaint;
        private Paint infoPaint;
        private Paint shadowPaint;
        private Paint borderPaint;
        private float currentAzimuth = 0;

        private String[] mainDirections;
        private String[] subDirections;
        private String magneticNorthText;

        public CompassView(Context context) {
            super(context);
            initLanguage();
            initPaints();
        }

        private void initLanguage() {

            String language = Locale.getDefault().getLanguage();

            if (language.equals("ru")) {

                mainDirections = new String[]{"СЕВЕР", "ВОСТОК", "ЮГ", "ЗАПАД"};
                subDirections = new String[]{"СВ", "ЮВ", "ЮЗ", "СЗ"};
                magneticNorthText = "Магнитный север";
            } else {

                mainDirections = new String[]{"NORTH", "EAST", "SOUTH", "WEST"};
                subDirections = new String[]{"NE", "SE", "SW", "NW"};
                magneticNorthText = "Magnetic North";
            }
        }

        private void initPaints() {
            // Фон
            backgroundPaint = new Paint();
            backgroundPaint.setColor(Color.rgb(15, 20, 40));
            backgroundPaint.setStyle(Paint.Style.FILL);
            backgroundPaint.setAntiAlias(true);

            // Краска для теней
            shadowPaint = new Paint();
            shadowPaint.setColor(Color.argb(150, 0, 0, 0));
            shadowPaint.setStyle(Paint.Style.FILL);
            shadowPaint.setAntiAlias(true);

            // Краска для обводки стрелок
            borderPaint = new Paint();
            borderPaint.setColor(Color.WHITE);
            borderPaint.setStyle(Paint.Style.STROKE);
            borderPaint.setStrokeWidth(3);
            borderPaint.setAntiAlias(true);

            // Основной текст (внутри стрелок)
            mainTextPaint = new Paint();
            mainTextPaint.setColor(Color.WHITE);
            mainTextPaint.setTextSize(48);
            mainTextPaint.setTextAlign(Align.CENTER);
            mainTextPaint.setAntiAlias(true);
            mainTextPaint.setFakeBoldText(true);

            // Второстепенный текст
            Paint subTextPaint = new Paint();
            subTextPaint.setColor(Color.WHITE);
            subTextPaint.setTextSize(36);
            subTextPaint.setTextAlign(Align.CENTER);
            subTextPaint.setAntiAlias(true);

            // Деления
            tickPaint = new Paint();
            tickPaint.setColor(Color.WHITE);
            tickPaint.setAntiAlias(true);

            // Центральная точка
            centerPaint = new Paint();
            centerPaint.setColor(Color.BLACK);
            centerPaint.setStyle(Paint.Style.FILL);
            centerPaint.setAntiAlias(true);

            // Информационный текст
            infoPaint = new Paint();
            infoPaint.setColor(Color.CYAN);
            infoPaint.setTextSize(40);
            infoPaint.setTextAlign(Align.CENTER);
            infoPaint.setAntiAlias(true);
            infoPaint.setFakeBoldText(true);
        }

        public void updateAzimuth(float azimuth) {
            this.currentAzimuth = azimuth;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            int width = getWidth();
            int height = getHeight();
            int centerX = width / 2;
            int centerY = height / 2;
            int radius = Math.min(centerX, centerY) - 40;

            // Фон
            canvas.drawColor(Color.BLACK);
            canvas.drawCircle(centerX, centerY, radius + 10, backgroundPaint);


            canvas.save();
            canvas.rotate(-currentAzimuth, centerX, centerY);

            // Рисуем деления
            drawTicks(canvas, centerX, centerY, radius);

            // Рисуем объемные стрелки розы ветров
            drawVolumetricWindRose(canvas, centerX, centerY, radius);

            // Восстанавливаем canvas
            canvas.restore();

            // Рисуем центральную точку
            drawCenterPoint(canvas, centerX, centerY);

            // Рисуем текущее направление
            drawCurrentDirection(canvas, centerX, centerY, radius);
        }

        private void drawTicks(Canvas canvas, int centerX, int centerY, int radius) {
            // Основные деления (каждые 30 градусов)
            for (int i = 0; i < 360; i += 30) {
                double rad = Math.toRadians(i);
                float x1 = centerX + (float) Math.sin(rad) * (radius - 10);
                float y1 = centerY - (float) Math.cos(rad) * (radius - 10);
                float x2 = centerX + (float) Math.sin(rad) * (radius - 25);
                float y2 = centerY - (float) Math.cos(rad) * (radius - 25);

                tickPaint.setStrokeWidth(3);
                tickPaint.setColor(Color.WHITE);
                canvas.drawLine(x1, y1, x2, y2, tickPaint);
            }

            // Цифры градусов (каждые 30 градусов)
            tickPaint.setTextSize(28);
            tickPaint.setTextAlign(Align.CENTER);
            for (int i = 0; i < 360; i += 30) {
                if (i == 0) continue; // 0 градусов не рисуем
                double rad = Math.toRadians(i);
                float x = centerX + (float) Math.sin(rad) * (radius - 45);
                float y = centerY - (float) Math.cos(rad) * (radius - 45) + 10;
                canvas.drawText(String.valueOf(i), x, y, tickPaint);
            }
        }

        private void drawVolumetricWindRose(Canvas canvas, int centerX, int centerY, int radius) {
            int arrowLength = radius - 30;
            int arrowWidth = 60;
            int arrowDepth = 40; // Глубина объемности

            // Основные направления
            float[] mainAngles = {0, 90, 180, 270};
            int[] mainColors = {
                Color.rgb(220, 60, 60),    // Красный для севера
                Color.rgb(60, 220, 60),    // Зеленый для востока
                Color.rgb(160, 60, 220),   // Синий для юга
                Color.rgb(220, 220, 60)    // Желтый для запада
            };

            // Промежуточные направления
            float[] subAngles = {45, 135, 225, 315};
            int subColor = Color.rgb(180, 140, 60); // Золотистый

            // Рисуем стрелки (начиная с промежуточных, чтобы основные были сверху)
            for (int i = 0; i < subAngles.length; i++) {
                drawVolumetricArrow(canvas, centerX, centerY, subAngles[i], 
									arrowLength, arrowWidth - 10, arrowDepth - 10, subColor, 
									subDirections[i], 36);
            }

            for (int i = 0; i < mainAngles.length; i++) {
                drawVolumetricArrow(canvas, centerX, centerY, mainAngles[i], 
									arrowLength + 20, arrowWidth, arrowDepth, mainColors[i], 
									mainDirections[i], 48);
            }
        }

        private void drawVolumetricArrow(Canvas canvas, int centerX, int centerY, 
										 float angle, int length, int width, 
										 int depth, int color, String text, float textSize) {
            double rad = Math.toRadians(angle);

            // Координаты кончика стрелки
            float tipX = centerX + (float) Math.sin(rad) * length;
            float tipY = centerY - (float) Math.cos(rad) * length;

            // Координаты основания стрелки
            float baseX1 = centerX + (float) Math.sin(rad + Math.PI/2) * width/2;
            float baseY1 = centerY - (float) Math.cos(rad + Math.PI/2) * width/2;
            float baseX2 = centerX + (float) Math.sin(rad - Math.PI/2) * width/2;
            float baseY2 = centerY - (float) Math.cos(rad - Math.PI/2) * width/2;

            // Координаты для объемности (задняя часть стрелки)
            float backTipX = tipX - (float) Math.sin(rad) * depth;
            float backTipY = tipY + (float) Math.cos(rad) * depth;
            float backBaseX1 = baseX1 - (float) Math.sin(rad) * depth;
            float backBaseY1 = baseY1 + (float) Math.cos(rad) * depth;
            float backBaseX2 = baseX2 - (float) Math.sin(rad) * depth;
            float backBaseY2 = baseY2 + (float) Math.cos(rad) * depth;

            // Создаем градиент для объемного эффекта
            LinearGradient gradient = new LinearGradient(
                tipX, tipY, backTipX, backTipY,
                color, Color.rgb(
                    Math.max(0, Color.red(color) - 80),
                    Math.max(0, Color.green(color) - 80),
                    Math.max(0, Color.blue(color) - 80)
                ),
                Shader.TileMode.CLAMP
            );

            Paint arrowPaint = new Paint();
            arrowPaint.setShader(gradient);
            arrowPaint.setAntiAlias(true);

            // Рисуем заднюю грань
            Path backFace = new Path();
            backFace.moveTo(backTipX, backTipY);
            backFace.lineTo(backBaseX1, backBaseY1);
            backFace.lineTo(backBaseX2, backBaseY2);
            backFace.close();

            // Более темный цвет для задней грани
            Paint backPaint = new Paint();
            backPaint.setColor(Color.rgb(
								   Math.max(0, Color.red(color) - 100),
								   Math.max(0, Color.green(color) - 100),
								   Math.max(0, Color.blue(color) - 100)
							   ));
            backPaint.setAntiAlias(true);

            canvas.drawPath(backFace, backPaint);

            // Рисуем боковые грани
            Path sideFace1 = new Path();
            sideFace1.moveTo(tipX, tipY);
            sideFace1.lineTo(backTipX, backTipY);
            sideFace1.lineTo(backBaseX1, backBaseY1);
            sideFace1.lineTo(baseX1, baseY1);
            sideFace1.close();

            Path sideFace2 = new Path();
            sideFace2.moveTo(tipX, tipY);
            sideFace2.lineTo(backTipX, backTipY);
            sideFace2.lineTo(backBaseX2, backBaseY2);
            sideFace2.lineTo(baseX2, baseY2);
            sideFace2.close();

            Paint sidePaint = new Paint();
            sidePaint.setColor(Color.rgb(
								   Math.max(0, Color.red(color) - 50),
								   Math.max(0, Color.green(color) - 50),
								   Math.max(0, Color.blue(color) - 50)
							   ));
            sidePaint.setAntiAlias(true);

            canvas.drawPath(sideFace1, sidePaint);
            canvas.drawPath(sideFace2, sidePaint);

            // Рисуем переднюю грань (основная стрелка)
            Path frontFace = new Path();
            frontFace.moveTo(tipX, tipY);
            frontFace.lineTo(baseX1, baseY1);
            frontFace.lineTo(baseX2, baseY2);
            frontFace.close();

            canvas.drawPath(frontFace, arrowPaint);

            // Обводка стрелки
            canvas.drawPath(frontFace, borderPaint);

            // Рисуем текст внутри стрелки
            float textX = tipX - (float) Math.sin(rad) * (depth/2 + length/3);
            float textY = tipY + (float) Math.cos(rad) * (depth/2 + length/3);

            // Поворачиваем текст в соответствии с направлением
            canvas.save();
            canvas.rotate(angle, textX, textY);
            if (angle > 90 && angle < 270) {
                canvas.rotate(180, textX, textY);
            }

            mainTextPaint.setTextSize(textSize);
            mainTextPaint.setColor(Color.WHITE);

            // Добавляем тень тексту
            Paint textShadowPaint = new Paint();
            textShadowPaint.set(mainTextPaint);
            textShadowPaint.setColor(Color.BLACK);
            textShadowPaint.setStrokeWidth(6);
            textShadowPaint.setStyle(Paint.Style.STROKE);

            canvas.drawText(text, textX, textY + textSize/3, textShadowPaint);
            canvas.drawText(text, textX, textY + textSize/3, mainTextPaint);

            canvas.restore();
        }

        private void drawCenterPoint(Canvas canvas, int centerX, int centerY) {
            // Внешний круг
            centerPaint.setColor(Color.rgb(60, 60, 80));
            canvas.drawCircle(centerX, centerY, 25, centerPaint);

            // Средний круг
            centerPaint.setColor(Color.BLACK);
            canvas.drawCircle(centerX, centerY, 20, centerPaint);

            // Внутренний круг с градиентом
            Paint gradientPaint = new Paint();
            RadialGradient radialGradient = new RadialGradient(
                centerX, centerY, 15,
                Color.rgb(100, 100, 120), Color.BLACK,
                Shader.TileMode.CLAMP
            );
            gradientPaint.setShader(radialGradient);
            canvas.drawCircle(centerX, centerY, 15, gradientPaint);

            // Центральная точка
            centerPaint.setColor(Color.WHITE);
            canvas.drawCircle(centerX, centerY, 6, centerPaint);

            // Крест в центре
            Paint crossPaint = new Paint();
            crossPaint.setColor(Color.RED);
            crossPaint.setStrokeWidth(4);
            crossPaint.setAntiAlias(true);

            canvas.drawLine(centerX - 12, centerY, centerX + 12, centerY, crossPaint);
            canvas.drawLine(centerX, centerY - 12, centerX, centerY + 12, crossPaint);
        }

        private void drawCurrentDirection(Canvas canvas, int centerX, int centerY, int radius) {
            // Фон для информации
            Paint bgPaint = new Paint();
            bgPaint.setColor(Color.argb(200, 30, 35, 60));
            bgPaint.setStyle(Paint.Style.FILL);
            RectF infoRect = new RectF(centerX - 200, centerY + radius - 60, 
									   centerX + 200, centerY + radius + 60);
            canvas.drawRoundRect(infoRect, 20, 20, bgPaint);

            // Рамка с градиентом
            Paint framePaint = new Paint();
            framePaint.setStyle(Paint.Style.STROKE);
            framePaint.setStrokeWidth(5);
            LinearGradient frameGradient = new LinearGradient(
                centerX - 200, centerY + radius, centerX + 200, centerY + radius,
                Color.CYAN, Color.BLUE, Shader.TileMode.CLAMP
            );
            framePaint.setShader(frameGradient);
            canvas.drawRoundRect(infoRect, 20, 20, framePaint);

            // Текущее направление
            String directionName = getDirectionName(currentAzimuth);
            String info = String.format("%.0f° %s", currentAzimuth, directionName);

            infoPaint.setColor(Color.WHITE);
            infoPaint.setTextSize(46);

            // Тень текста
            Paint textShadow = new Paint();
            textShadow.set(infoPaint);
            textShadow.setColor(Color.BLACK);
            textShadow.setStrokeWidth(8);
            textShadow.setStyle(Paint.Style.STROKE);

            canvas.drawText(info, centerX, centerY + radius + 15, textShadow);
            canvas.drawText(info, centerX, centerY + radius + 15, infoPaint);

            // Магнитный север
            infoPaint.setTextSize(32);
            infoPaint.setColor(Color.YELLOW);
            canvas.drawText(magneticNorthText, centerX, centerY + radius - 80, infoPaint);
        }

        private String getDirectionName(float degrees) {
            if (Locale.getDefault().getLanguage().equals("ru")) {
                if (degrees >= 337.5 || degrees < 22.5) return "СЕВЕР";
                if (degrees >= 22.5 && degrees < 67.5) return "СЕВЕРО-ВОСТОК";
                if (degrees >= 67.5 && degrees < 112.5) return "ВОСТОК";
                if (degrees >= 112.5 && degrees < 157.5) return "ЮГО-ВОСТОК";
                if (degrees >= 157.5 && degrees < 202.5) return "ЮГ";
                if (degrees >= 202.5 && degrees < 247.5) return "ЮГО-ЗАПАД";
                if (degrees >= 247.5 && degrees < 292.5) return "ЗАПАД";
                return "СЕВЕРО-ЗАПАД";
            } else {
                if (degrees >= 337.5 || degrees < 22.5) return "NORTH";
                if (degrees >= 22.5 && degrees < 67.5) return "NORTH-EAST";
                if (degrees >= 67.5 && degrees < 112.5) return "EAST";
                if (degrees >= 112.5 && degrees < 157.5) return "SOUTH-EAST";
                if (degrees >= 157.5 && degrees < 202.5) return "SOUTH";
                if (degrees >= 202.5 && degrees < 247.5) return "SOUTH-WEST";
                if (degrees >= 247.5 && degrees < 292.5) return "WEST";
                return "NORTH-WEST";
            }
        }
    }

    // Внутренний класс для радиального градиента (так как RadialGradient может быть не в API 6)
    static class RadialGradient extends Shader {
        private int centerColor;
        private int edgeColor;
        private float centerX;
        private float centerY;
        private float radius;

        public RadialGradient(float centerX, float centerY, float radius,
							  int centerColor, int edgeColor, Shader.TileMode tileMode) {
            this.centerX = centerX;
            this.centerY = centerY;
            this.radius = radius;
            this.centerColor = centerColor;
            this.edgeColor = edgeColor;
        }
    }
}