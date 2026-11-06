import os
import glob
import shutil


path_to_dir = "dataset"
path_to_dir_save = f"{path_to_dir}_norm"
# os.makedirs(path_to_dir_save, exist_ok=True)
THRESHOLD = 30

class_list = os.listdir(path_to_dir)
for class_name in class_list:
    class_dir = os.path.join(path_to_dir, class_name)
    if os.path.isdir(class_dir):
        os.makedirs(os.path.join(path_to_dir_save, class_name), exist_ok=True)
        img_paths = glob.glob(os.path.join(class_dir, "*.wav"))
        c = 0
        for img_path in img_paths:
            if c >= THRESHOLD: break
            shutil.copy(img_path, os.path.join(path_to_dir_save, class_name, os.path.basename(img_path)))
            # img_name = os.path.splitext(os.path.basename(img_path))[0]
            # shutil.copy(img_path, os.path.join(path_to_dir_save, img_name + class_name + ".jpg"))
            c += 1
        print(f"Copied {c}/{len(img_paths)} images in {class_dir}")