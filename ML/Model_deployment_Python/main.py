import fastai as fst
import pickle
import numpy as np
import torch
from fastai.vision.all import *
from flask import Flask, request, jsonify

with open(file = 'doctor.pickle', mode = 'rb') as file:
    model = pickle.load(file = file)
model

app = Flask(__name__)

@app.route("/", methods = ["GET", "POST"])
def index():
    if request.method == "POST":
        file = request.files.get(key = 'file')
        if file is None or file.filename == "":
            return jsonify({"error": "No file part"})
        
        try:
            image_bytes = file.read()
            pillow_img = PILImage.create(io.BytesIO(image_bytes))
            pred, pred_id, probs = model.predict(item = pillow_img)
            data = {str(pred) : float(probs[pred_id])}
            return data
        except Exception as e:
            return jsonify({"error" : str(e)})
        
    return "OK"

if __name__ == "__main__":
    app.run(debug = True)

