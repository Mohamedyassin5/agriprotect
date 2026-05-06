from fastapi import FastAPI, UploadFile, File, HTTPException
import numpy as np
import cv2
import face_recognition

app = FastAPI(title="Agri Face Service")

async def read_rgb(upload: UploadFile):
    data = await upload.read()
    if not data:
        raise HTTPException(status_code=400, detail="Empty image")

    nparr = np.frombuffer(data, np.uint8)
    img = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
    if img is None:
        raise HTTPException(status_code=400, detail="Invalid image format")

    return cv2.cvtColor(img, cv2.COLOR_BGR2RGB)

def get_largest_face_encoding(img):
    locations = face_recognition.face_locations(img)
    if not locations:
        return None

    # loc is (top, right, bottom, left). Area is (bottom - top) * (right - left)
    largest_loc = max(locations, key=lambda loc: (loc[2] - loc[0]) * (loc[1] - loc[3]))

    # removed num_jitters to fix the 20000ms timeout error! model="large" is default and accurate.
    encodings = face_recognition.face_encodings(img, known_face_locations=[largest_loc], model="large")
    return encodings[0] if encodings else None

@app.post("/compare")
async def compare(file1: UploadFile = File(...), file2: UploadFile = File(...)):
    try:
        img1 = await read_rgb(file1)
        img2 = await read_rgb(file2)

        enc1 = get_largest_face_encoding(img1)
        enc2 = get_largest_face_encoding(img2)

        if enc1 is None:
            return {"match": False, "distance": None, "error": "No face found in image 1"}
        if enc2 is None:
            return {"match": False, "distance": None, "error": "No face found in image 2"}

        # Sweet spot tolerance
        results = face_recognition.compare_faces([enc1], enc2, tolerance=0.48)
        dist = face_recognition.face_distance([enc1], enc2)

        return {"match": bool(results[0]), "distance": float(dist[0]), "error": None}
    except Exception as e:
         return {"match": False, "distance": None, "error": str(e)}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8001)
