package com.maven.scorescanner;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;

import com.maven.scorescanner.utils.PlayMedia;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nathan on 5/25/2017.
 */

public class LoadFile extends AppCompatActivity {

    private static final String TAG = "Load File";
    private final int IMAGE_PICKER_REQUEST = 1;


    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");

                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };


    public LoadFile() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.load_file);

        final Button button1 =  (Button) findViewById(R.id.button1);
        final Button button2 = (Button) findViewById(R.id.button2);
        final ImageView imageView = (ImageView) findViewById(R.id.imageView1);

        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                int[] soundIDs = {R.raw.don_1, R.raw.re_1, R.raw.mi_1};
                PlayMedia playAudio = new PlayMedia(LoadFile.this,soundIDs);
                playAudio.execute();

            }
        });

        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI), IMAGE_PICKER_REQUEST);

            }
        });


        Log.i(TAG, "called onCreate");
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == IMAGE_PICKER_REQUEST && resultCode == RESULT_OK) {
            String filename = getRealPathFromURI(data.getData());

            Mat img = Imgcodecs.imread(filename);

            Log.i("image URL", filename);
            Log.i("images mat",img.size().toString() );

            img = getPoints(img);

            // convert to bitmap:
            Bitmap bm = Bitmap.createBitmap(img.width(), img.height(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(img, bm);

            //     find the imageview and draw it!
            ImageView iv = (ImageView) findViewById(R.id.imageView1);
            iv.setImageBitmap(bm);

        }
    }

    private String getRealPathFromURI(final Uri contentUri) {
        final String[] proj = { MediaStore.Images.Media.DATA };
        final Cursor cursor = managedQuery(contentUri, proj, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();

        return cursor.getString(column_index);
    }

    @Override
    public void onPause()
    {
        super.onPause();
    }
    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public Mat getPoints(Mat inputFrame) {

        Mat grey = new Mat();
        // Make it greyscale
        Imgproc.cvtColor(inputFrame, grey, Imgproc.COLOR_RGBA2GRAY);

        // init contours arraylist
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>(200);

        // because findContours modifies the image I back it up
        Mat greyCopy = new Mat();
        grey.copyTo(greyCopy);

        //blur
        Imgproc.GaussianBlur(greyCopy, greyCopy, new Size(5,5), 2);
        Imgproc.adaptiveThreshold(grey, grey, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY_INV, 15, 4);

        Imgproc.findContours(greyCopy, contours, new Mat(), Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_NONE);
        // Now I have my controus pefectly

        MatOfPoint2f mMOP2f1 = new MatOfPoint2f();

        //a list for only selected contours
        List<MatOfPoint> SelectedContours = new ArrayList<MatOfPoint>(400);

        double h_point = 0;
        double l_point = 0;
        int count_size = 0;
        int biggest_id  = 0;

        for(int i=0;i<contours.size();i++)
        {

            MatOfPoint m = contours.get(i);
            List<Point> list = m.toList();
            if (count_size < list.size()){
                count_size = list.size();
                biggest_id = i;
            }
            for(int j = 0; j<list.size(); j++) {
                Point p = list.get(j);

                if(p.y > h_point){
                    h_point = p.y;
                    l_point = p.x;
                }
            }
            SelectedContours.add(contours.get(i));
        }

        /* Getting staves Location on image */

        List<MatOfPoint> staves = new ArrayList<MatOfPoint>(400);
        Mat lines = new Mat();

        Imgproc.HoughLinesP(grey, lines, 1 , Math.PI/180, 80, 400, 10);

        for (int x = 0; x < lines.rows(); x++)
        {
            double[] vec = lines.get(x,0);
            double x1 = vec[0],
                    y1 = vec[1],
                    x2 = vec[2],
                    y2 = vec[3];
            Point start = new Point(x1, y1);
            Point end = new Point(x2, y2);
            double dx = x1 - x2;
            double dy = y1 - y2;

            System.out.println(TAG+" Point Coordinates: x1=>"+x1+" y1=>"+y1+" x2=>"+x2+" y2=>"+y2);
            double dist = Math.sqrt (dx*dx + dy*dy);

            if(dist>300.d)  // show those lines that have length greater than 300
                Imgproc.line(inputFrame, start, end, new Scalar(255,0, 0, 255),1);// here initimg is the original image.

        }
        Imgproc.drawContours(inputFrame, SelectedContours, -1, new Scalar(0,255,0,255), 2);

        return inputFrame;
    }

}
