/*
TODO Automatyczne budowanie modelu koloru
 */
package detekcjaObiektow;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.paint.Color;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
/** Klasa kontrolera aplikacji.*/
public class FXMLUIController implements Initializable {

    @FXML
    private Button start_btn;
    @FXML
    private ImageView currentFrame;
    @FXML
    private ImageView morphImgL;
    @FXML
    private ImageView morphImgR;

    @FXML
    private Slider hueMinL;
    @FXML
    private Slider hueMaxL;
    @FXML
    private Slider satMinL;
    @FXML
    private Slider satMaxL;
    @FXML
    private Slider valMinL;
    @FXML
    private Slider valMaxL;

    @FXML
    private Slider hueMinR;
    @FXML
    private Slider hueMaxR;
    @FXML
    private Slider satMinR;
    @FXML
    private Slider satMaxR;
    @FXML
    private Slider valMinR;
    @FXML
    private Slider valMaxR;

    @FXML
    private ColorPicker colorPickerL;
    @FXML
    private ColorPicker colorPickerR;

    protected ScheduledExecutorService tim;
    protected VideoCapture capt = new VideoCapture();
    protected boolean camActive = false;
    private static int camId = 0;
    private Color c;
    private Color d;
    private Color[] sample = new Color[100];
    private Color sampleApprox;
    private double r = 0, g = 0, b = 0;
    public PixelReader pr;
    private Image img;
    private int pixelCounter;

    /** Uruchamia konstruktor dla zmiennej przechowującej obiekt kamery*/
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        this.capt = new VideoCapture();
    }

    /** Odpowiada za funkcje przycisku start/stop uruchomienia kamery. Przypisuje do zmiennych wartości kolorów z ColorPickerów*/
    @FXML
    protected void startCamera(ActionEvent e) {
        c = colorPickerL.getValue();
        d = colorPickerR.getValue();

        if (this.camActive == false) {
            this.capt.open(camId);
            this.camActive = true;
            Runnable frameGrabber = () -> {
                Mat frame = grabFrame();
                Image imgToShow = ImageHandling.mat2Image(frame);
                updateImg(currentFrame, imgToShow);
            };

            this.tim = Executors.newSingleThreadScheduledExecutor();
            this.tim.scheduleAtFixedRate(frameGrabber, 0, 33, TimeUnit.MILLISECONDS);
            /** Obsługa wykrywania obiektów poprzez ich zaznaczenie kursorem na ekranie*/
            currentFrame.setOnMousePressed(ev -> {

                System.out.println((int) ev.getX() + "-" + (int) ev.getY());
                img = currentFrame.getImage();
                System.out.println((int) img.getWidth() + "-" + (int) img.getHeight());

                pr = img.getPixelReader();
            /** Zbudowanie modelu koloru na podstawie  zaznaczonego piksela i maski 5x5 wokół niego. Z kolorów wyciągana jest wartość uśrediona i następnie zapisywana do modelu*/
                pixelCounter = 0;
                for (int coordY = -2; coordY <= 2; coordY++) {
                    for (int coordX = -2; coordX <= 2; coordX++) {
                        sample[pixelCounter] = pr.getColor((int) (ev.getX() * 2) + coordX, (int) (ev.getY() * 2) + coordY);
                        pixelCounter++;
                    }
                }

                pixelCounter--;

                while (pixelCounter >= 0) {
                    r = r + sample[pixelCounter].getRed();
                    g = g + sample[pixelCounter].getGreen();
                    b = b + sample[pixelCounter].getBlue();
                    pixelCounter--;
                }

                r = r / 25;
                g = g / 25;
                b = b / 25;

                sampleApprox = Color.color(r, g, b);
                //System.out.println("R:" + sampleApprox.getRed() + "\nG:" + sampleApprox.getGreen() + "\nB:" + sampleApprox.getBlue());

                /** Detekcja naciśnięcia lewego lub prawego przycisku myszy. Zależnie od naciśnięcia budowany jest model dla obiektu pierwszego lub drugiego*/
                if (ev.isPrimaryButtonDown()) {
                    colorPickerL.setValue(sampleApprox);
                    ChangeSlidersL();
                } else if (ev.isSecondaryButtonDown()) {
                    colorPickerR.setValue(sampleApprox);
                    ChangeSlidersR();
                }
                r = 0;
                g = 0;
                b = 0;
            });

            this.start_btn.setText("Stop");
        } else {
            this.currentFrame.setImage(null);
            this.stopAcquisition();
            this.start_btn.setText("Start");
            this.camActive = false;
        }

    }
/** Metoda odpowiada za ustalenie wartości dla modelu obiektu pierwszego przy z wykorzystaniem suwaków. Dodatkowo ustalone są pewne przedziały ułatwiające detekcję.*/
    public void ChangeSlidersL() {
        c = colorPickerL.getValue();
        if (c.getHue() < 10) {
            hueMinL.setValue(c.getHue() / 2);
            hueMaxL.setValue(c.getHue() / 2 + 10);
        } else if (c.getHue() > 350) {
            hueMinL.setValue(c.getHue() / 2 - 10);
            hueMaxL.setValue(c.getHue() / 2);
        } else {
            hueMinL.setValue(c.getHue() / 2 - 5);
            hueMaxL.setValue(c.getHue() / 2 + 5);
        }

        if ((c.getSaturation() * 255) < 50) {
            satMinL.setValue(0);
            satMaxL.setValue(100);
        } else if ((c.getSaturation() * 255) > 205) {
            satMinL.setValue(155);
            satMaxL.setValue(255);
        } else {
            satMinL.setValue((c.getSaturation() * 255) - 50);
            satMaxL.setValue((c.getSaturation() * 255) + 50);
        }

        if ((c.getBrightness() * 255) < 50) {
            valMinL.setValue(0);
            valMaxL.setValue(100);
        } else if ((c.getBrightness() * 255) > 205) {
            valMinL.setValue(155);
            valMaxL.setValue(255);
        } else {
            valMinL.setValue((c.getBrightness() * 255) - 50);
            valMaxL.setValue((c.getBrightness() * 255) + 50);
        }

    }
/** Metoda odpowiada za ustalenie wartości dla modelu obiektu drugiego przy z wykorzystaniem suwaków. Dodatkowo ustalone są pewne przedziały ułatwiające detekcję.*/
    public void ChangeSlidersR() {
        d = colorPickerR.getValue();
        if (d.getHue() < 10) {
            hueMinR.setValue(d.getHue() / 2);
            hueMaxR.setValue(d.getHue() / 2 + 10);
        } else if (c.getHue() > 350) {
            hueMinR.setValue(d.getHue() / 2 - 10);
            hueMaxR.setValue(d.getHue() / 2);
        } else {
            hueMinR.setValue(d.getHue() / 2 - 5);
            hueMaxR.setValue(d.getHue() / 2 + 5);
        }

        if ((c.getSaturation() * 255) < 50) {
            satMinR.setValue(0);
            satMaxR.setValue(100);
        } else if ((c.getSaturation() * 255) > 205) {
            satMinR.setValue(155);
            satMaxR.setValue(255);
        } else {
            satMinR.setValue((c.getSaturation() * 255) - 50);
            satMaxR.setValue((c.getSaturation() * 255) + 50);
        }

        if ((c.getBrightness() * 255) < 50) {
            valMinR.setValue(0);
            valMaxR.setValue(100);
        } else if ((c.getBrightness() * 255) > 205) {
            valMinR.setValue(155);
            valMaxR.setValue(255);
        } else {
            valMinL.setValue((c.getBrightness() * 255) - 50);
            valMaxL.setValue((c.getBrightness() * 255) + 50);
        }
    }
/** Metoda odczytuje aktualną klatkę z kamery i poddaje ją analizie i obróbce.*/
    private Mat grabFrame() {

        Mat frame = new Mat();
        Mat blurredImg = new Mat();
        Mat hsvImg = new Mat();
        Mat mask = new Mat();
        Mat morphOutputL = new Mat();
        Mat morphOutputR = new Mat();

        this.capt.read(frame);

        //obsługa sliderów
        Scalar minVL = new Scalar(this.hueMinL.getValue(), this.satMinL.getValue(), this.valMinL.getValue());
        Scalar maxVL = new Scalar(this.hueMaxL.getValue(), this.satMaxL.getValue(), this.valMaxL.getValue());

        Scalar minVR = new Scalar(this.hueMinR.getValue(), this.satMinR.getValue(), this.valMinR.getValue());
        Scalar maxVR = new Scalar(this.hueMaxR.getValue(), this.satMaxR.getValue(), this.valMaxR.getValue());

        Mat dilateElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(24, 24));
        Mat erodeElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(12, 12));

        //usuwa szum
        Imgproc.blur(frame, blurredImg, new Size(7, 7));

        //Lewy obraz
        //zamienia na HSV, erozja+dylatacja
        Imgproc.cvtColor(blurredImg, hsvImg, Imgproc.COLOR_BGR2HSV);

        Core.inRange(hsvImg, minVL, maxVL, mask);

        Imgproc.erode(mask, morphOutputL, erodeElement);
        Imgproc.erode(mask, morphOutputL, erodeElement);

        Imgproc.dilate(mask, morphOutputL, dilateElement);
        Imgproc.dilate(mask, morphOutputL, dilateElement);

        this.updateImg(this.morphImgL, ImageHandling.mat2Image(morphOutputL));

        //prawy obraz
        //zamienia na HSV, erozja+dylatacja
        Imgproc.cvtColor(blurredImg, hsvImg, Imgproc.COLOR_BGR2HSV);

        Core.inRange(hsvImg, minVR, maxVR, mask);

        Imgproc.erode(mask, morphOutputR, erodeElement);
        Imgproc.erode(mask, morphOutputR, erodeElement);

        Imgproc.dilate(mask, morphOutputR, dilateElement);
        Imgproc.dilate(mask, morphOutputR, dilateElement);

        this.updateImg(this.morphImgR, ImageHandling.mat2Image(morphOutputR));

        frame = this.detect(morphOutputL, frame, new Scalar(c.invert().getBlue() * 255, c.invert().getGreen() * 255, c.invert().getRed() * 255));
        frame = this.detect(morphOutputR, frame, new Scalar(d.invert().getBlue() * 255, d.invert().getGreen() * 255, d.invert().getRed() * 255));

        return frame;
    }

    //* Metoda aktualizacji obrazu z kamery*/
    private void updateImg(ImageView view, Image img) {
        ImageHandling.onFXThread(view.imageProperty(), img);
    }

    private Mat detect(Mat m, Mat frame, Scalar color) {
        List<MatOfPoint> ctr = new ArrayList<>();
        Mat hie = new Mat();
        Imgproc.findContours(m, ctr, hie, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE);

        if (hie.size().height > 0 && hie.size().width > 0) {
            for (int x = 0; x >= 0; x = (int) hie.get(0, x)[0]) {
                Imgproc.drawContours(frame, ctr, x, color, 3);
            }
        }
        return frame;
    }
/** Metoda odpowiedzialna za zakończenie odczytu obrazu z kamery, jest wywoływana w metodzie Start*/
    protected void stopAcquisition() {
        if (this.tim != null && !this.tim.isShutdown()) {
            try {

                this.tim.shutdown();
                this.tim.awaitTermination(33, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {

                System.err.println(e);
            }
        }

        if (this.capt.isOpened()) {

            this.capt.release();

            
        }
    }

}
