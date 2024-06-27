const express = require('express');
const app = express();
const port = 5555; 

const axios = require("axios");
const Jimp = require("jimp");
const tesseract = require("node-tesseract-ocr");

async function getCaptchaData() {
  const url =
    "https://gateway-voters.eci.gov.in/api/v1/captcha-service/generateCaptcha";
  const headers = {
    Accept: "application/json, text/plain, */*",
    Origin: "https://electoralsearch.eci.gov.in",
    appName: "ELECTORAL_SEARCH",
  };

  try {
    const response = await axios.get(url, { headers });
    const data = response.data;

    if (data.status === "Success" && data.statusCode === 200) {
      return {
        captcha: data.captcha,
        id: data.id,
      };
    } else {
      console.log("Invalid response");
      return null;
    }
  } catch (error) {
    console.error(error.toString());
    return null;
  }
}

async function getVoterInfo(epicno, captchaId, captchaData) {
  const url =
    "https://gateway.eci.gov.in/api/v1/elastic/search-by-epic-from-national-display";
  const headers = {
    Accept: "application/json, text/plain, */*",
    "Content-Type": "application/json",
    Origin: "https://electoralsearch.eci.gov.in",
    applicationName: "ELECTORAL_SEARCH",
  };

  const data = {
    isPortal: true,
    epicNumber: epicno,
    captchaId: captchaId,
    captchaData: captchaData,
    securityKey: "na",
  };

  console.log(data);

  try {
    const response = await axios.post(url, data);
    if (response.status === 200) {
      return response.data;
    } else {
      console.log(`Failed to get data. Status code: ${response.statusText}`);
      return null;
    }
  } catch (error) {
    console.error(`An error occurred: ${error.toString()}`);
    return null;
  }
}
async function getCode(base64Image) {
  try {
    const imageBuffer = Buffer.from(base64Image, "base64");
    const image = await Jimp.read(imageBuffer);
    const filePath = "captcha_image.jpg";
    await image.writeAsync(filePath);
    console.log("Image saved as captcha_image.jpg");

    const text = await tesseract.recognize(filePath, {
      lang: "eng",
      oem: 1,
      psm: 3,
    });

    return text.toLowerCase();
  } catch (error) {
    console.error(
      `An error occurred while processing the image: ${error.toString()}`
    );
    return "";
  }
}

async function getUserInfo(epicno) {
  let gotData = false;
  let count = 0;

  if (count > 16) {
    console.log(`Too many attempts`);
    return "";
  }

  while (!gotData && count <= 15) {
    const captchaData = await getCaptchaData();
    if (!captchaData) break;

    const { captcha, id: captchaId } = captchaData;
    const captchaDataProcessed = await getCode(captcha);

    console.log(captchaDataProcessed);
    console.log(epicno, captchaId, captchaDataProcessed);

    const data = await getVoterInfo(
      epicno,
      captchaId,
      captchaDataProcessed.trim()
    );
    if (data !== null) {
      gotData = true;
      console.log(data);
      return data;
    } else {
      console.log("wrong captcha");
    }
    ++count;
  }
  
}

// async function getUserInfo(epicno).then((data) => {
//   if (data) {
//     console.log("Voter Information:", data);
//   }
// });


app.use(express.json()); // To support JSON-encoded bodies

// API endpoint
app.get('/getVoterInfo/:epicno', async (req, res) => {
    const epicno = req.params.epicno;
    
    try {
        const data = await getUserInfo(epicno);
        if (data) {
            res.json(data);
        } else {
            res.status(404).json({ message: 'Data not found' });
        }
    } catch (error) {
        res.status(500).json({ message: 'Internal Server Error' });
    }
});

app.get('/', async (req, res) => {
  
  try {
      const data = await getUserInfo(epicno);
      if (data) {
          res.json(data);
      } else {
          res.status(404).json({ message: 'Hello User' });
      }
  } catch (error) {
      res.status(500).json({ message: 'Internal Server Error' });
  }
});

app.listen(5555, () => {
  console.log('Server running on port 5555') 
})