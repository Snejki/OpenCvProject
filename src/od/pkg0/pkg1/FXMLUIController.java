/*
TODO śledzenie obiektów 2 różnych kolorach
 */
package od.pkg0.pkg1;

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
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.Objdetect;
import org.opencv.videoio.VideoCapture;

public class FXMLUIController implements Initializable {

    @FXML
    private Button start_btn;
    @FXML
    private ImageView currentFrame;
    @FXML
    private ImageView maskImgL;
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

    private ScheduledExecutorService tim;
    private VideoCapture capt = new VideoCapture();
    private boolean camActive = false;
    private static int camId = 0;

    private CascadeClassifier faceCascade;
    private int absFaceSize;

    @FXML
    protected void startCamera(ActionEvent e) {

        if (this.camActive == false) {
            this.capt.open(camId);
            this.camActive = true;
            Runnable frameGrabber = () -> {
                Mat frame = grabFrame();
                Image imgToShow = Utils.mat2Image(frame);
                updateImg(currentFrame, imgToShow);
                //updateImg(maskImg, imgToShow);
                //updateImg(morphImg, imgToShow);
            };

            this.tim = Executors.newSingleThreadScheduledExecutor();
            this.tim.scheduleAtFixedRate(frameGrabber, 0, 33, TimeUnit.MILLISECONDS);

            this.start_btn.setText("Stop");
        } else {
            this.currentFrame.setImage(null);
            this.stopAcquisition();
            this.start_btn.setText("Start");
            this.camActive = false;
        }

    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        this.capt = new VideoCapture();
        this.faceCascade = new CascadeClassifier();
        this.absFaceSize = 0;
        this.faceCascade.load("resources/haarcascades/haarcascade_frontalface_alt.xml");

    }

    private Mat grabFrame() {

        Mat frame = new Mat();
        Mat blurredImg = new Mat();
        Mat hsvImg = new Mat();
        Mat mask = new Mat();
        Mat morphOutputL = new Mat();
        Mat morphOutputR = new Mat();

        this.capt.read(frame);

        //usuwa szum
        Imgproc.blur(frame, blurredImg, new Size(7, 7));

        //obsługa sliderów
        Scalar minVL = new Scalar(this.hueMinL.getValue(), this.satMinL.getValue(), this.valMinL.getValue());
        Scalar maxVL = new Scalar(this.hueMaxL.getValue(), this.satMaxL.getValue(), this.valMaxL.getValue());

        Scalar minVR = new Scalar(this.hueMinR.getValue(), this.satMinR.getValue(), this.valMinR.getValue());
        Scalar maxVR = new Scalar(this.hueMaxR.getValue(), this.satMaxR.getValue(), this.valMaxR.getValue());

        Mat dilateElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(24, 24));
        Mat erodeElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(12, 12));


        //Lewy obraz
            //zamienia na HSV
            Imgproc.cvtColor(blurredImg, hsvImg, Imgproc.COLOR_BGR2HSV);
            
            Core.inRange(hsvImg, minVL, maxVL, mask);
            
            Imgproc.erode(mask, morphOutputL, erodeElement);
            Imgproc.erode(mask, morphOutputL, erodeElement);

            Imgproc.dilate(mask, morphOutputL, dilateElement);
            Imgproc.dilate(mask, morphOutputL, dilateElement);

            this.updateImg(this.morphImgL, Utils.mat2Image(morphOutputL));

        //prawy obraz
            //zamienia na HSV
            Imgproc.cvtColor(blurredImg, hsvImg, Imgproc.COLOR_BGR2HSV);
            
            Core.inRange(hsvImg, minVR, maxVR, mask);
            
            Imgproc.erode(mask, morphOutputR, erodeElement);
            Imgproc.erode(mask, morphOutputR, erodeElement);

            Imgproc.dilate(mask, morphOutputR, dilateElement);
            Imgproc.dilate(mask, morphOutputR, dilateElement);

            this.updateImg(this.morphImgR, Utils.mat2Image(morphOutputR));

        
            
        frame = this.detect(morphOutputL, frame, new Scalar(0, 255, 0));
        frame = this.detect(morphOutputR, frame, new Scalar(255, 0, 0));

        return frame;
    }

    private void updateImg(ImageView view, Image img) {
        Utils.onFXThread(view.imageProperty(), img);
    }

    private Mat detect(Mat m, Mat frame, Scalar color) {
        List<MatOfPoint> ctr = new ArrayList<>();
        Mat hie = new Mat();
        Imgproc.findContours(m, ctr, hie, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE);

        if (hie.size().height > 0 && hie.size().width > 0) {
            for (int x = 0; x >= 0; x = (int) hie.get(0, x)[0]) {
                Imgproc.drawContours(frame, ctr, x, color);
            }
        }
        return frame;
    }

    private void stopAcquisition() {
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

    private void detectAndDisplay(Mat frame) {
        MatOfRect faces = new MatOfRect();
        Mat grayFrame = new Mat();

        Imgproc.cvtColor(frame, grayFrame, Imgproc.COLOR_BGR2GRAY);
        Imgproc.equalizeHist(grayFrame, grayFrame);

        if (this.absFaceSize == 0) {
            int wys = grayFrame.rows();
            if (Math.round(wys * 0.2f) > 0) {
                this.absFaceSize = Math.round(wys * 0.2f);
            }
        }

        this.faceCascade.detectMultiScale(grayFrame, faces, 1.1, 2, Objdetect.CASCADE_SCALE_IMAGE, new Size(this.absFaceSize, this.absFaceSize), new Size());

        Rect[] facesA = faces.toArray();
        for (int i = 0; i < facesA.length; i++) {
            Imgproc.rectangle(frame, facesA[i].tl(), facesA[i].br(), new Scalar(255, 255, 255), 3);
        }
    }

}
