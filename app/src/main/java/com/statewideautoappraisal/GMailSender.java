package com.statewideautoappraisal;

/**
 * Created by techiestown on 1/10/16.
 */

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Environment;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.security.Security;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

public class GMailSender extends javax.mail.Authenticator {
    static {
        Security.addProvider(new JSSEProvider());
    }

    Activity activity;
    private String mailhost = "smtp.gmail.com";
    private String user;
    private String password;
    private Session session;
    private Multipart _multipart;

    public GMailSender(Activity activity, final String user, final String password) {
        this.user = user;
        this.password = password;
        this.activity = activity;
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

         session = Session.getInstance(props,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(user, password);
                    }
                });
        _multipart = new MimeMultipart();
    }

    protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication(user, password);
    }

    public synchronized boolean sendMail(final String subject, final String body, final String sender, final String recipients) throws Exception {
        new AsyncTask<Void, Void, String>() {
            ProgressDialog mProgressDialog;

            @Override
            protected String doInBackground(Void... params) {
                // TODO Auto-generated method stub
                try {


                    MimeMessage message = new MimeMessage(session);
                    DataHandler handler = new DataHandler(new ByteArrayDataSource(body.getBytes(), "text/plain"));
                    message.setSender(new InternetAddress(sender));
                    message.setSubject(subject);

                    message.setDataHandler(handler);

                    BodyPart messageBodyPart = new MimeBodyPart();
                    messageBodyPart.setText(body);
                    _multipart.addBodyPart(messageBodyPart);

                    message.setContent(_multipart);

                    if (recipients.indexOf(',') > 0) {
                        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipients));
                    } else {
                        message.setRecipient(Message.RecipientType.TO, new InternetAddress(recipients));
                    }
                    Transport.send(message);

                    return "true";
                } catch (Exception e) {
                    try {
                        Writer writer = new StringWriter();
                        PrintWriter printWriter = new PrintWriter(writer);
                        e.printStackTrace(printWriter);
                        String s = writer.toString();

                        File myFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/AUTOAPPRIASAL/Log2.txt");
                        myFile.createNewFile();
                        FileOutputStream fOut = new FileOutputStream(myFile);
                        OutputStreamWriter myOutWriter =
                                new OutputStreamWriter(fOut);
                        myOutWriter.append("printstacktrace:"+s+"\nmessage:" + e.getMessage()+"\ncause:"+e.getCause()+"\nstacktrace:"+e.getStackTrace()+"localized message:"+e.getLocalizedMessage());
                        myOutWriter.close();
                        fOut.close();
                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }
                    e.printStackTrace();
                    return "false";
                }

            }

            @Override
            protected void onPostExecute(String result) {
                // TODO Auto-generated method stub
                super.onPostExecute(result);
                mProgressDialog.dismiss();
                if (result.equalsIgnoreCase("true")) {
                    Toast.makeText(activity, "Thank you! Your report submitted successfully", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(activity, "Oops! we can't send your report \nPlease try again", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            protected void onPreExecute() {
                // TODO Auto-generated method stub
                super.onPreExecute();
                mProgressDialog = new ProgressDialog(activity);
                mProgressDialog.setTitle("");
                mProgressDialog.setCanceledOnTouchOutside(false);
                mProgressDialog.setMessage("Please Wait...");
                mProgressDialog.show();

            }
        }.execute();
        return true;
    }

    public void addAttachment(String filename, String subject) throws Exception {
        BodyPart messageBodyPart = new MimeBodyPart();
        DataSource source = new FileDataSource(filename);
        messageBodyPart.setDataHandler(new DataHandler(source));
        messageBodyPart.setFileName(filename);
        _multipart.addBodyPart(messageBodyPart);

//        BodyPart messageBodyPart2 = new MimeBodyPart();
//        messageBodyPart2.setText(subject);
//
//        _multipart.addBodyPart(messageBodyPart2);
    }

    public class ByteArrayDataSource implements DataSource {
        private byte[] data;
        private String type;

        public ByteArrayDataSource(byte[] data, String type) {
            super();
            this.data = data;
            this.type = type;
        }

        public ByteArrayDataSource(byte[] data) {
            super();
            this.data = data;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getContentType() {
            if (type == null)
                return "application/octet-stream";
            else
                return type;
        }

        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(data);
        }

        public String getName() {
            return "ByteArrayDataSource";
        }

        public OutputStream getOutputStream() throws IOException {
            throw new IOException("Not Supported");
        }
    }
}