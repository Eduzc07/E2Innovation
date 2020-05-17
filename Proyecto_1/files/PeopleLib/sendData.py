#!/usr/bin/env python
# -*- coding: utf-8 -*
import datetime
import smtplib
from email.mime.multipart import MIMEMultipart
from email.mime.text import MIMEText
from email.MIMEBase import MIMEBase
from email import encoders

import csv
import os
import MySQLdb

#---------------------------------------------
fromaddr = 'ezarate@e2i.com.pe'
#toaddrs = 'edu.zc07@gmail.com'
# toaddrs = 'erodriguez@e2i.com.pe'
# toaddrs = 'jdongo@e2i.com.pe'
# toaddrs = 'edu_zac@hotmail.com'
# toaddrs = 'e.zarate@pucp.pe'

username = 'ezarate@e2i.com.pe'
password = 'e2i2016'
serverName = 'seguridad.hostingcorporativo.com'
port =  465
report = "[Mail Service]:"
#---------------------------------------------
full_path = os.path.realpath(__file__)
Filepath = os.path.dirname(full_path)

configFile = "%s/../../Data/CustomerData.csv"%Filepath
if os.path.isfile(configFile):
    print("%s configFile has been Found!"%report)
    csv_data = csv.reader(file(configFile))
    for row in csv_data:
        if(row[0] =="Correo"):
            toaddrs = row[1] # ID-name from the bus
else:
    print("%s configFile does not exist."%report)
    

global Mail
Mail = False #True: Enviado False: No enviado

def isTime():
    global Mail
    now = datetime.datetime.now()
    if(now.hour == 23):
        Mail = False

    if(now.hour == 0 and not Mail):
        print ("%s Report will be sent today %s. "%(report,now.strftime("%A, %d. %B %Y %I:%M%p")))
        return True
    return False

    # print ("Current date and time using instance attributes:")
    # print ("Current year: %d" % now.year)
    # print ("Current month: %d" % now.month)
    # print ("Current day: %d" % now.day)
    # print ("Current hour: %d" % now.hour)
    # 
    # print ("Current second: %d" % now.second)
    # print ("Current microsecond: %d" % now.microsecond)

def createMail(msg):
    yesterday = datetime.datetime.now() - datetime.timedelta(days=1)
    msg['Subject'] = "People Counting Report %s"%yesterday.strftime("%d/%m/20%y %H:%M:%S")
    # msg['From'] = 'E2Innovation'
    msg['From'] = fromaddr
    msg['To'] = toaddrs

    # Create the body of the message (a plain-text and an HTML version).
    html = """\
    <html>
      <head></head>
      <body>
        <p>Good Night Dear Customer:<br><br>
           The report from the current day has been sent.<br><br>
           More information. <a href="https://www.e2i.com.pe">E2I Innovation</a>
        </p>
      </body>
    </html>
    """

    # Record the MIME types of both parts - text/plain and text/html.
    partText = MIMEText(html, 'html')

    # Attach parts into message container.
    # According to RFC 2046, the last part of a multipart message, in this case
    # the HTML message, is best and preferred.
    msg.attach(partText)
    return msg

def loadData(msg,attachment,filename):
    part = MIMEBase('application', 'octet-stream')
    part.set_payload((attachment).read())
    encoders.encode_base64(part)
    part.add_header('Content-Disposition', "attachment; filename=%s"%filename)
    msg.attach(part)
    attachment.close()
    return msg

def sendMail():
    global Mail
    yesterday = datetime.datetime.now() - datetime.timedelta(days=1)
    if (not isTime()):
        return False

    filename = "data_%s.csv"%(yesterday.strftime("%d_%m"))
    path = "%s/../../Data/%s"%(Filepath,filename)

    if os.path.isfile(path):
        attachment = open(path, "rb")
    else:
        if not Mail:
            print("%s There is not exist data from %s."%(report,yesterday))
        Mail = True     # Marcar como si se hubiera enviado
        return True     # Volver a plotear el header

    print("%s Creating Mail ..."%report)
    msg = MIMEMultipart('mixed')
    msg = createMail(msg)
    print("%s Loading data ..."%report)
    msg = loadData(msg,attachment,filename)
    print("%s Connecting server ..."%report)
    server = smtplib.SMTP_SSL(serverName, port)
    server.login(username, password)
    print("%s Sending data to %s..."%(report,toaddrs))
    server.sendmail(fromaddr, toaddrs, msg.as_string())
    print("%s The data from %s has been sent."%(report,yesterday.strftime("%d/%m/20%y")))
    server.quit()
    print("%s The data will be uploaded."%report)
    # sendServerData()

    Mail = True
    return True
    

def sendServerData():
    db = MySQLdb.connect("www.e2i.com.pe",
                         "olcqsxdf_py",
                         "Kdgmd100!",
                         "olcqsxdf_pygt")
    cursor = db.cursor()

    yesterday = datetime.datetime.now() - datetime.timedelta(days=1)
    filename = "data_%s.csv"%(yesterday.strftime("%d_%m"))
    path = "%s/../../Data/%s"%(Filepath,filename)

    csv_data = csv.reader(file(path))

    for row in csv_data:
        sql = """INSERT INTO data 
            (equipo, fechahora, subio, bajo, latitud, longitud)
            VALUES
            ('%s','%s','%s','%s','%s','%s') 
            """% (row[0],row[1],row[2],row[3],row[4],row[5])
        
        #In order to not sent the name of each column
        if(row[0]!="equipo"):
            cursor.execute(sql)
        
    db.commit()
    db.close()
    print ("%s Data has been sent to Server E2I"%report) 


##--------------------------------------------------------
def createMailMonth(msg,month,day,toaddrsDir):
    yesterday = datetime.datetime.now() - datetime.timedelta(days=1)
    msg['Subject'] = "People Counting Report %02d/%02d/2017"%(day,month)
    # msg['From'] = 'E2Innovation'
    msg['From'] = fromaddr
    msg['To'] = toaddrsDir

    # Create the body of the message (a plain-text and an HTML version).
    html = """\
    <html>
      <head></head>
      <body>
        <p>Good Night Dear Customer:<br><br>
           The report from the current day has been sent.<br><br>
           More information. <a href="https://www.e2i.com.pe">E2I Innovation</a>
        </p>
      </body>
    </html>
    """

    # Record the MIME types of both parts - text/plain and text/html.
    partText = MIMEText(html, 'html')

    # Attach parts into message container.
    # According to RFC 2046, the last part of a multipart message, in this case
    # the HTML message, is best and preferred.
    msg.attach(partText)
    return msg

def sendMailMonth(month,day,toaddrsDir):
    global Mail
        
    filename = "data_%02d_%02d.csv"%(day,month)
    path = "%s/../../Data/%s"%(Filepath,filename)

    if os.path.isfile(path):
        attachment = open(path, "rb")
    else:
        if not Mail:
            print("%s There is not exist data from %02d/%02d."%(report,day,month))
        Mail = True     # Marcar como si se hubiera enviado
        return True     # Volver a plotear el header

    print("%s Creating Mail ..."%report)
    msg = MIMEMultipart('mixed')
    msg = createMailMonth(msg,month,day,toaddrsDir)
    print("%s Loading data ..."%report)
    msg = loadData(msg,attachment,filename)
    print("%s Connecting server ..."%report)
    server = smtplib.SMTP_SSL(serverName, port)
    server.login(username, password)
    print("%s Sending data to %s..."%(report,toaddrsDir))
    server.sendmail(fromaddr, toaddrsDir, msg.as_string())
    print("%s The data from %02d/%02d has been sent."%(report,day,month))
    server.quit()
    # print("%s The data will be uploaded."%report)
    # sendServerDataMonth(month,day)

    Mail = True
    return True
    

def sendServerDataMonth(month,day):
    db = MySQLdb.connect(host = "www.e2i.com.pe",
                         user = "olcqsxdf_py",
                         passwd = "Kdgmd100!",
                         db = "olcqsxdf_pygt",
                         connect_timeout = 10000)
    cursor = db.cursor()

    filename = "data_%02d_%02d.csv"%(day,month)
    path = "%s/../../Data/%s"%(Filepath,filename)

    csv_data = csv.reader(file(path))

    for row in csv_data:
        sql = """INSERT INTO data 
            (equipo, fechahora, subio, bajo, latitud, longitud)
            VALUES
            ('%s','%s','%s','%s','%s','%s') 
            """% (row[0],row[1],row[2],row[3],row[4],row[5])
        
        print("sendig: %s -> %s "%(row[0],row[1]))
        #In order to not sent the name of each column
        if(row[0]!="equipo"):
            cursor.execute(sql)
        
    db.commit()
    db.close()
    print ("%s Data from %02d_%02d has been sent to Server E2I"%(report,day,month))
