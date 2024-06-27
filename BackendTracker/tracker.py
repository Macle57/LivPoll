import cv2
import imutils
import pyrebase
import threading
import os
from dotenv import load_dotenv
load_dotenv()

# Firebase configuration
config = {
    'apiKey': os.getenv('apiKey'),
    'authDomain': os.getenv('authDomain'),
    'databaseURL': os.getenv('databaseURL'),
    'projectId': os.getenv('projectId'),
    'storageBucket': os.getenv('storageBucket'),
    'messagingSenderId': os.getenv('messagingSenderId'),
    'appId': os.getenv('appId')
}


def update_firebase(count):
    """Function to update Firebase database in a separate thread."""
    db.child("people").update({"Name": str(count)})


# Initialize Firebase
firebase = pyrebase.initialize_app(config)
db = firebase.database()

# Initialize the HOG person detector
hog = cv2.HOGDescriptor()
hog.setSVMDetector(cv2.HOGDescriptor_getDefaultPeopleDetector())

# Start the video capture
cap = cv2.VideoCapture(1)  # Using camera at index 0

while cap.isOpened():
    ret, image = cap.read()
    if ret:
        # Resize the image
        image = imutils.resize(image, width=min(400, image.shape[1]))

        # Detect pedestrians
        (regions, _) = hog.detectMultiScale(image, winStride=(4, 4), padding=(4, 4), scale=1.05)

        # Draw rectangles around detected pedestrians
        pedestrian_count = len(regions)
        for (x, y, w, h) in regions:
            cv2.rectangle(image, (x, y), (x + w, y + h), (0, 0, 255), 2)

        # Display the count on the image
        org = (50, 50)
        font = cv2.FONT_HERSHEY_SIMPLEX
        cv2.putText(image, f"Count: {pedestrian_count}", org, font, 1, (255, 0, 0), 2, cv2.LINE_AA)

        # Update the database asynchronously
        # update_firebase(pedestrian_count)
        threading.Thread(target=update_firebase, args=(pedestrian_count,)).start()

        # Display the resulting frame
        cv2.imshow("Pedestrian Detection", image)

        # Break the loop with 'q'
        if cv2.waitKey(25) & 0xFF == ord('q'):
            break
    else:
        break

# Release the capture and destroy all windows
cap.release()
cv2.destroyAllWindows()
