package com.siecola.exemploeandroid;

import java.io.IOException;

import com.siecola.exemploeandroid.gcm.GCMRegister;
import com.siecola.exemploeandroid.gcm.GCMRegisterEvents;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements GCMRegisterEvents {

	private String registrationID;
	private GCMRegister gcmRegister;
	private Button btnUnregister;
	private Button btnRegister;
	private Button btnClearMessage;
	private EditText edtSenderID;
	private TextView txtMessage;
	private TextView edtRegistrationID;
	
	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		edtSenderID = (EditText) findViewById(R.id.edtSenderID);
		txtMessage = (TextView) findViewById(R.id.txtMessage);
		btnRegister = (Button) findViewById(R.id.btnRegister);
		btnUnregister = (Button) findViewById(R.id.btnUnregister);
		btnClearMessage = (Button) findViewById(R.id.btnClearMessage);
		edtRegistrationID = (TextView) findViewById(R.id.txtRegistrationID);

		if (gcmRegister == null)
			gcmRegister = new GCMRegister(this);
	
		edtSenderID.setText(gcmRegister.getSenderId());
		
		if (!gcmRegister.isRegistrationExpired()) {
			registrationID = gcmRegister.getCurrentRegistrationId();
			setForRegistered(registrationID);
		} else {
			setForUnregistered();
		}

		String messageRX = (String) getIntent().getSerializableExtra("messageRX");
		if (messageRX != null) {
			txtMessage.setText(messageRX);
		}

		btnRegister.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
								
				registrationID = gcmRegister.getRegistrationId(edtSenderID.getText().toString());
				
				if ((registrationID == null) || (registrationID.length() == 0)) {
					Toast.makeText(MainActivity.this, "Dispositivo ainda não registrado na nuvem. Tentando...", Toast.LENGTH_SHORT).show();
					setForUnregistered();
				}
				else {
					Toast.makeText(MainActivity.this, "Dispositivo já registrado na nuvem.", Toast.LENGTH_SHORT).show();
					setForRegistered(registrationID);
				}				
			}												
		});
		
		btnUnregister.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				gcmRegister.unRegister();
			}
		});
		
		btnClearMessage.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				txtMessage.setText("");	
			}
		});
				
	}
	
	private void setForRegistered (String regID) {
		edtRegistrationID.setText(regID);
		btnUnregister.setEnabled(true);
		btnRegister.setEnabled(false);
	}
	
	private void setForUnregistered () {
		edtRegistrationID.setText("");
		btnUnregister.setEnabled(false);
		btnRegister.setEnabled(true);
	}
	
	@Override
	protected void onNewIntent(Intent intent) {
		String messageRX = (String) intent.getSerializableExtra("messageRX");
		if (messageRX != null) {
			txtMessage.setText(messageRX);
		}
		super.onNewIntent(intent);
	}	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
		
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
	}

	@Override
	public void gcmRegisterFinished(String registrationID) {
		Toast.makeText(this, "Dispositivo registrado na nuvem com sucesso.", Toast.LENGTH_SHORT).show();
		setForRegistered(registrationID);
	}

	@Override
	public void gcmRegisterFailed(IOException ex) {
		Toast.makeText(this, "Falha ao registrar dispositivo na nuvem. " + ex.getMessage(), Toast.LENGTH_SHORT).show();
		setForUnregistered();
	}

	@Override
	public void gcmUnregisterFinished() {
		Toast.makeText(MainActivity.this, "Dispositivo desregistrado da nuvem.", Toast.LENGTH_SHORT).show();
		setForUnregistered();
	}

	@Override
	public void gcmUnregisterFailed(IOException ex) {
		Toast.makeText(MainActivity.this, "Falha ao desregistrar o dispositivo na nuvem.", Toast.LENGTH_SHORT).show();							
	}
}
