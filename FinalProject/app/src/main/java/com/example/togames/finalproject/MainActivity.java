package com.example.togames.finalproject;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.content.FileProvider;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends AppCompatActivity implements View.OnClickListener,
        StepsFragment.OnFragmentInteractionListener, WeightFragment.OnFragmentInteractionListener,
        HeartFragment.OnFragmentInteractionListener, NavigationView.OnNavigationItemSelectedListener {

    private static final int FRAGMENT_WEIGHT = 0;
    private static final int FRAGMENT_STEPS = 1;
    private static final int FRAGMENT_HEART = 2;

    protected static final int REQUEST_TAKE_PHOTO = 1, REQUEST_CROP_PHOTO = 2, REQUEST_PICK_PHOTO = 3,
            REQUEST_THEME = 4;

    private static final int NUM_PAGES = 3;

    private static final String FRAGMENT_NUMBER = "fragment_number";
    private static final String SAVED_EMAIL = "email", SAVED_NAME = "name",
            SAVED_SURNAME = "surname", SAVED_AGE = "age", SAVED_WEIGHT = "weight",
            SAVED_HEIGHT = "height", SAVED_STEP_GOAL = "step_goal", SAVED_BITMAP = "bitmap";

    protected static final String GALLERY_PICK = "gallery_pick", THEME_PICK = "theme_pick";

    private FirebaseAuth auth;
    private TextView textView_name, textView_nav_name;
    private ImageView imageView_user, imageView_weight, imageView_steps, imageView_heart;
    private ImageButton imageButton_settings;
    private ImageView[] bottomViews;
    private CircleImageView profile_image;
    private DrawerLayout drawerLayout;
    private User user;
    private int fragmentPosition;
    private FirebaseDatabase database;
    private Fragment[] fragments;
    private ViewPager mPager;
    // The pager adapter, which provides the pages to the view pager widget.
    private PagerAdapter mPagerAdapter;
    private PackageManager packageManager;
    private String mCurrentPhotoPath;
    private Uri contentUri;
    private Bitmap profile_image_bitmap;
    private BitmapChangeListener bitmapChangeListener;
    private File storageDir;
    private @ColorInt
    int color;
    private boolean isDark;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        isDark = AppSettings.getInstance(this).isDarkTheme;
        setTheme(isDark ? R.style.NoTitleThemeDark : R.style.NoTitleTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = getTheme();
        theme.resolveAttribute(R.attr.colorPrimary, typedValue, true);
        color = typedValue.data;

        textView_name = findViewById(R.id.textView_name);
        imageView_user = findViewById(R.id.imageView_user);
        imageView_weight = findViewById(R.id.imageView_weight);
        imageView_steps = findViewById(R.id.imageView_steps);
        imageView_heart = findViewById(R.id.imageView_heart);
        imageButton_settings = findViewById(R.id.imageButton_location);
        drawerLayout = findViewById(R.id.drawerLayout);

        NavigationView navigationView = findViewById(R.id.navigationView);
        View hView = navigationView.getHeaderView(0);
        textView_nav_name = hView.findViewById(R.id.nav_name);
        profile_image = hView.findViewById(R.id.profile_image);
        navigationView.setNavigationItemSelectedListener(this);

        imageButton_settings.setOnClickListener(this);
        imageView_user.setOnClickListener(this);
        imageView_weight.setOnClickListener(this);
        imageView_steps.setOnClickListener(this);
        imageView_heart.setOnClickListener(this);
        profile_image.setOnClickListener(this);

        // Store the bottom bar views in an array
        // (to be able to iterate over them when updating the GUI)
        bottomViews = new ImageView[]{imageView_weight, imageView_steps, imageView_heart};

        // Initialize the directory to store images
        storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        // Initialize package manager
        packageManager = getPackageManager();

        // Initialize the fragments array
        fragments = new Fragment[NUM_PAGES];
        fragments[FRAGMENT_WEIGHT] = WeightFragment.getInstance();
        fragments[FRAGMENT_STEPS] = StepsFragment.getInstance();
        fragments[FRAGMENT_HEART] = HeartFragment.getInstance();

        // Instantiate a ViewPager and a PagerAdapter.
        mPager = findViewById(R.id.pager);
        instantiateViewPager();

        // Initialize Firebase Authenticator
        auth = FirebaseAuth.getInstance();

        // Check if there is a savedInstanceState
        if (savedInstanceState != null) {
            fragmentPosition = savedInstanceState.getInt(FRAGMENT_NUMBER);
            changeFragment();
            updateBottomBar();
            user = getUserFromLocalData();
            if (user != null) {
                updateGUI();
                //return;
            }
        } else {
            fragmentPosition = FRAGMENT_STEPS;
            changeFragment();
            updateBottomBar();
        }

        // Load user data from Firebase
        database = FirebaseDatabase.getInstance();
        String userId = auth.getCurrentUser().getUid();
        database.getReference().child("user_data").child(userId).
                addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        // Data available. Load data and update GUI
                        user = dataSnapshot.getValue(User.class);
                        if (user != null) {
                            updateGUI();
                            Toast.makeText(MainActivity.this, R.string.data_loaded,
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Toast.makeText(MainActivity.this, R.string.database_error,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }


    /**
     * A simple pager adapter
     */
    private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {
        public ScreenSlidePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return fragments[position];
        }

        @NonNull
        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Object ret = super.instantiateItem(container, position);
            fragments[position] = (Fragment) ret;
            return ret;
        }

        @Override
        public int getCount() {
            return NUM_PAGES;
        }
    }

    private synchronized void updateGUI() {
        // Write user's name and surname into the top text view
        String nameSurname = user.getName() + " " + user.getSurname();
        textView_name.setText(nameSurname);
        textView_nav_name.setText(nameSurname);

        // Send the new data to WeightFragment
        ((WeightFragment) fragments[FRAGMENT_WEIGHT]).setData(user.getWeight(), user.getHeight());
        ((StepsFragment) fragments[FRAGMENT_STEPS]).setData(user.getStepGoal());

        // Decode the encoded image and update the profile photo
        decodeImage();
        profile_image.setImageBitmap(profile_image_bitmap);
    }

    private void instantiateViewPager() {
        // Instantiate a ViewPager and a PagerAdapter.
        mPagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
        mPager.setOffscreenPageLimit(NUM_PAGES - 1);
        mPager.setAdapter(mPagerAdapter);
        mPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                fragmentPosition = position;
                updateBottomBar();
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.imageView_user) { // User icon is clicked
            //updateGUI();
            drawerLayout.openDrawer(GravityCompat.START);
        } else if (id == R.id.imageView_weight) { // Weight icon is clicked
            if (fragmentPosition == FRAGMENT_WEIGHT) return;
            fragmentPosition = FRAGMENT_WEIGHT;
            changeFragment();
        } else if (id == R.id.imageView_steps) { // Steps icon is clicked
            if (fragmentPosition == FRAGMENT_STEPS) return;
            fragmentPosition = FRAGMENT_STEPS;
            changeFragment();
        } else if (id == R.id.imageView_heart) { // Heart icon is clicked
            if (fragmentPosition == FRAGMENT_HEART) return;
            fragmentPosition = FRAGMENT_HEART;
            changeFragment();
        } else if (id == R.id.imageButton_location) {
            Intent intent_maps = new Intent(MainActivity.this, MapsActivity.class);
            startActivity(intent_maps);
        } else if (id == R.id.profile_image) {
            if (profile_image_bitmap != null) {
                buildImageDialog();
            } else {
                buildPhotoIntent();
            }
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // set item as selected to persist highlight
        // item.setChecked(true);
        // close drawer when item is tapped
        drawerLayout.closeDrawers();

        switch (item.getItemId()) {
            case R.id.nav_camera:
                buildPhotoIntent();
                break;
            case R.id.nav_gallery:
                buildGalleryIntent();
                break;
            case R.id.nav_audio:
                buildAudioIntent();
                break;
            case R.id.nav_settings:
                buildSettingsIntent();
                break;
            case R.id.nav_logout:
                // Sign out, finish current activity and go back to LoginActivity
                auth.signOut();
                buildLogoutIntent();
                finish();
                break;
            default:
                return false;
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_CANCELED) return;
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
            drawerLayout.openDrawer(GravityCompat.START);
            galleryAddPic();
            getBitmapFromPath();
            cropImage();
            buildImageDialog();
        } else if (requestCode == REQUEST_CROP_PHOTO && resultCode == RESULT_OK) {
            if (data != null) {
                Bundle bundle = data.getExtras();
                profile_image_bitmap = bundle.getParcelable("data");
                broadcastBitmapChange();
            }
        } else if (requestCode == REQUEST_PICK_PHOTO) {
            drawerLayout.openDrawer(GravityCompat.START);
            String path = data.getStringExtra(GALLERY_PICK);
            mCurrentPhotoPath = path;
            File f = new File(mCurrentPhotoPath);
            contentUri = Uri.fromFile(f);
            getBitmapFromPath();
            buildImageDialog();
        } else if (requestCode == REQUEST_THEME) {
            boolean isDarkPicked = data.getBooleanExtra(THEME_PICK, isDark);
            if (isDark != isDarkPicked) {
                recreate();
            }
        }
    }

    private void broadcastBitmapChange() {
        if (bitmapChangeListener != null) {
            bitmapChangeListener.onBitmapChange(profile_image_bitmap);
        }
    }

    private void buildImageDialog() {
        final ImageDialog dialog = new ImageDialog(this, profile_image_bitmap);
        bitmapChangeListener = dialog;
        final Button buttonOK = dialog.findViewById(R.id.buttonOK);
        final Button buttonCrop = dialog.findViewById(R.id.buttonCrop);
        final Button buttonBlur = dialog.findViewById(R.id.buttonBlur);
        final Button buttonGrey = dialog.findViewById(R.id.buttonGrey);
        buttonOK.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                dialog.dismiss();
                if (profile_image_bitmap != null) {
                    profile_image.setImageBitmap(profile_image_bitmap);
                }
            }
        });
        buttonCrop.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                buttonCrop.setEnabled(false);
                buttonBlur.setEnabled(false);
                buttonGrey.setEnabled(false);
                buttonOK.setEnabled(false);
                cropImage();
                buttonCrop.setEnabled(true);
                buttonBlur.setEnabled(true);
                buttonGrey.setEnabled(true);
                buttonOK.setEnabled(true);
            }
        });
        buttonBlur.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                buttonCrop.setEnabled(false);
                buttonBlur.setEnabled(false);
                buttonGrey.setEnabled(false);
                buttonOK.setEnabled(false);
                blurImage();
                buttonCrop.setEnabled(true);
                buttonBlur.setEnabled(true);
                buttonGrey.setEnabled(true);
                buttonOK.setEnabled(true);
            }
        });
        buttonGrey.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                buttonCrop.setEnabled(false);
                buttonBlur.setEnabled(false);
                buttonGrey.setEnabled(false);
                buttonOK.setEnabled(false);
                getGrayscale();
                //invertImage();
                buttonCrop.setEnabled(true);
                buttonBlur.setEnabled(true);
                buttonGrey.setEnabled(true);
                buttonOK.setEnabled(true);
            }
        });

        dialog.show();
    }

    private void changeFragment() {
        // Replace the current fragment with a new one
        mPager.setCurrentItem(fragmentPosition);
        Log.d("changeFragment", "Fragment position: " + fragmentPosition);
    }

    public void updateBottomBar() {
        // Set the colors all views to transparent
        for (ImageView v : bottomViews) {
            v.setBackgroundColor(Color.TRANSPARENT);
        }
        Log.d("updateBottom", "View: " + bottomViews[fragmentPosition].toString());
        // Color the selected view
        bottomViews[fragmentPosition].setBackgroundColor(color);
    }

    @Override
    public void onFragmentInteraction(Uri uri) {
    }

    @Override
    public void onFragmentInteraction(int stepGoal) {
        user.setStepGoal(stepGoal);
    }

    @Override
    public void onFragmentInteraction(String weight, String height) {
        user.setWeight(weight);
        user.setHeight(height);
    }

    private User getUserFromLocalData() {
        // Loads user data using SharedPreferences
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        String email = sharedPref.getString(SAVED_EMAIL, null);
        if (email == null) return null;
        String name = sharedPref.getString(SAVED_NAME, null);
        String surname = sharedPref.getString(SAVED_SURNAME, null);
        String age = sharedPref.getString(SAVED_AGE, null);
        String weight = sharedPref.getString(SAVED_WEIGHT, null);
        String height = sharedPref.getString(SAVED_HEIGHT, null);
        int stepGoal = sharedPref.getInt(SAVED_STEP_GOAL, 0);
        String encodedImage = sharedPref.getString(SAVED_BITMAP, null);
        return new User(email, name, surname, age, weight, height, stepGoal, encodedImage);
    }

    private void saveUserDataIntoLocalStorage() {
        if (user == null) return;
        // Save user data using SharedPreferences
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.clear();
        editor.putString(SAVED_EMAIL, user.getEmail());
        editor.putString(SAVED_NAME, user.getName());
        editor.putString(SAVED_SURNAME, user.getSurname());
        editor.putString(SAVED_AGE, user.getAge());
        editor.putString(SAVED_WEIGHT, user.getWeight());
        editor.putString(SAVED_HEIGHT, user.getHeight());
        editor.putInt(SAVED_STEP_GOAL, user.getStepGoal());
        editor.putString(SAVED_BITMAP, encodeImage());
        editor.apply();
    }

    private void saveUserDataIntoFirebase() {
        if (user == null) return;
        user.setEncodedProfilePhoto(encodeImage());
        String userId = auth.getCurrentUser().getUid();
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().
                child("user_data").child(userId);
        userRef.setValue(user);
    }

    private String encodeImage() {
        if (profile_image_bitmap != null) {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            profile_image_bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            byte[] byteFormat = stream.toByteArray();
            return Base64.encodeToString(byteFormat, Base64.NO_WRAP);
        }
        return null;
    }

    private void decodeImage() {
        String code = user.getEncodedProfilePhoto();
        if (code == null || code.isEmpty()) return;
        byte[] decodedString = Base64.decode(user.getEncodedProfilePhoto(), Base64.DEFAULT);
        Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
        profile_image_bitmap = decodedByte;
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        saveUserDataIntoLocalStorage();
        saveUserDataIntoFirebase();
        savedInstanceState.putInt(FRAGMENT_NUMBER, fragmentPosition);
    }

    private void buildPhotoIntent() {
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)) return;
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.android.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        @SuppressLint("SimpleDateFormat")
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPG_" + timeStamp + "_";
        //storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(mCurrentPhotoPath);
        contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }

    private void getBitmapFromPath() {
        // Get the dimensions of the View
        int targetW = 300;
        int targetH = 300;

        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Determine how much to scale down the image
        int scaleFactor = Math.min(photoW / targetW, photoH / targetH);

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        profile_image_bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        //profile_image_bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath);
    }

    private void cropImage() {
        try {
            Intent cropIntent = new Intent("com.android.camera.action.CROP");
            cropIntent.setDataAndType(contentUri, "image/*");
            cropIntent.putExtra("crop", "true");
            cropIntent.putExtra("outputX", 300);
            cropIntent.putExtra("outputY", 300);
            cropIntent.putExtra("aspectX", 1);
            cropIntent.putExtra("aspectY", 1);
            cropIntent.putExtra("scaleUpIfNeeded", true);
            cropIntent.putExtra("return-data", true);
            startActivityForResult(cropIntent, REQUEST_CROP_PHOTO);
        } catch (ActivityNotFoundException ex) {

        }
    }

    private void blurImage() {
        profile_image_bitmap = BlurBuilder.blur(getApplicationContext(), profile_image_bitmap);
        broadcastBitmapChange();
    }

    private void invertImage() {
        int A, R, G, B;
        int pixelColor;
        int height = profile_image_bitmap.getHeight();
        int width = profile_image_bitmap.getWidth();

        Bitmap finalImage = Bitmap.createBitmap(width, height, profile_image_bitmap.getConfig());

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                pixelColor = profile_image_bitmap.getPixel(x, y);
                A = Color.alpha(pixelColor);
                R = 255 - Color.red(pixelColor);
                G = 255 - Color.blue(pixelColor);
                B = 255 - Color.green(pixelColor);
                finalImage.setPixel(x, y, Color.argb(A, R, G, B));
            }
        }
        profile_image_bitmap = finalImage;
        broadcastBitmapChange();
    }

    private void getGrayscale(){
        Bitmap src = profile_image_bitmap;
        //Custom color matrix to convert to GrayScale
        float[] matrix = new float[]{
                0.3f, 0.59f, 0.11f, 0, 0,
                0.3f, 0.59f, 0.11f, 0, 0,
                0.3f, 0.59f, 0.11f, 0, 0,
                0, 0, 0, 1, 0,};

        Bitmap dest = Bitmap.createBitmap(
                src.getWidth(),
                src.getHeight(),
                src.getConfig());

        Canvas canvas = new Canvas(dest);
        Paint paint = new Paint();
        ColorMatrixColorFilter filter = new ColorMatrixColorFilter(matrix);
        paint.setColorFilter(filter);
        canvas.drawBitmap(src, 0, 0, paint);

        profile_image_bitmap = dest;
        broadcastBitmapChange();
    }



    private void buildGalleryIntent() {
        File[] fileList = storageDir.listFiles(IMAGE_FILTER);
        ArrayList<String> paths = new ArrayList<>(fileList.length);

        for (File file : fileList) {
            paths.add(file.getAbsolutePath());
        }
        Intent gallery_intent = new Intent(MainActivity.this, GalleryActivity.class);
        gallery_intent.putStringArrayListExtra(GalleryActivity.IMAGE_PATHS, paths);
        startActivityForResult(gallery_intent, REQUEST_PICK_PHOTO);
    }

    // Array of supported extensions
    static final String[] EXTENSIONS = new String[]{
            "png", "bmp", "jpg" // supported formats
    };

    // Filter to identify images based on their extensions
    static final FilenameFilter IMAGE_FILTER = new FilenameFilter() {
        @Override
        public boolean accept(final File dir, final String name) {
            for (final String ext : EXTENSIONS) {
                if (name.endsWith("." + ext)) {
                    return (true);
                }
            }
            return (false);
        }
    };

    private void buildAudioIntent() {
        Intent audioIntent = new Intent(MainActivity.this, RecordActivity.class);
        startActivity(audioIntent);
    }

    private void buildSettingsIntent() {
        Intent settingsIntent = new Intent(MainActivity.this, SettingsActivity.class);
        startActivityForResult(settingsIntent, REQUEST_THEME);
    }

    private void buildLogoutIntent() {
        Intent logoutIntent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(logoutIntent);
    }

}
