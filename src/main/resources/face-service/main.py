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

@app.post("/compare")
async def compare(file1: UploadFile = File(...), file2: UploadFile = File(...)):
    try:
        img1 = await read_rgb(file1)
        img2 = await read_rgb(file2)

        enc1 = face_recognition.face_encodings(img1)
        enc2 = face_recognition.face_encodings(img2)

        if not enc1:
            return {"match": False, "distance": None, "error": "No face found in image 1"}
        if not enc2:
            return {"match": False, "distance": None, "error": "No face found in image 2"}

        # compare
        results = face_recognition.compare_faces([enc1[0]], enc2[0], tolerance=0.6)
        dist = face_recognition.face_distance([enc1[0]], enc2[0])

        return {"match": bool(results[0]), "distance": float(dist[0]), "error": None}
    except Exception as e:
         return {"match": False, "distance": None, "error": str(e)}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8001)
