package com.miguel.busunab.busunabmapa;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Created by MiguelSanchezG on 1/09/16.
 */
public class Info extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.informacion);

        final TextView text_correo_rene = (TextView)findViewById(R.id.text_correo_rene);
        final TextView text_correo_miguel = (TextView)findViewById(R.id.text_correo_miguel);
        TextView volverText = (TextView)findViewById(R.id.volverText);

        ImageView correo_rene = (ImageView)findViewById(R.id.correo_rene);
        ImageView correo_miguel = (ImageView)findViewById(R.id.correo_miguel);

        correo_rene.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
                emailIntent.setType("text/html");
                emailIntent.setData(Uri.parse("mailto:"));
                emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[] {text_correo_rene.getText().toString()});
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Escribe aquí tu mensaje");
                startActivity(Intent.createChooser(emailIntent, "Enviar correo"));
            }
        });

        correo_miguel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
                emailIntent.setType("text/html");
                emailIntent.setData(Uri.parse("mailto:"));
                emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[] {text_correo_miguel.getText().toString()});
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Escribe aquí tu mensaje");
                startActivity(Intent.createChooser(emailIntent, "Enviar correo"));
            }
        });

        volverText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }
}
