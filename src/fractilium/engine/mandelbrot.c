/* 
 * File:   mandelbrot.c
 * Author: Ari
 *
 * Created on 19 August 2012, 22:34
 */

#include "arfl.h"
#include "mandelbrot.h"
#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <math.h>
#include <mpfr.h>
#include <time.h>
#include <math.h>
#include <pthread.h>
#include <gsl/gsl_rng.h>
#include <gsl/gsl_randist.h>

#define DEFAULT_PRECISION (int32_t) 80;

mbrot_params *mbrotParams;
mbrot_stats *mbrotStats;
mpfr_t mbrotPlaneMinX, mbrotPlaneMaxX, mbrotPlaneMinY, mbrotPlaneMaxY, mbrotPlaneUnitX, mbrotPlaneUnitY;
int32_t(*mbrotColFunc)(int32_t, double);
double mbrotPlaneUnitXPri, mbrotPlaneUnitYPri;

double _toggleTimer() {
    static struct timeval oldTime;
    struct timeval newTime;
    double elapsedTime;

    /* Initialise the old_time if it has not yet been initialised. 
        This prevents problems with huge jumps in the first few ticks.*/
    if (oldTime.tv_sec == 0) {
        gettimeofday(&oldTime, NULL);
    }
    gettimeofday(&newTime, NULL);
    elapsedTime = (newTime.tv_sec - oldTime.tv_sec) + ((newTime.tv_usec - oldTime.tv_usec) / 1000000.0);
    oldTime = newTime;
    return elapsedTime;
}

int32_t _HSVToRGB(float hue, float saturation, float brightness) {
    int32_t r = 0, g = 0, b = 0;

    if (saturation == 0) {
        r = g = b = (int32_t) (brightness * 255.0f + 0.5f);
    } else {
        float h = (hue - (float) floor(hue)) * 6.0f;
        float f = h - (float) floor(h);
        float p = brightness * (1.0f - saturation);
        float q = brightness * (1.0f - saturation * f);
        float t = brightness * (1.0f - (saturation * (1.0f - f)));
        switch ((int32_t) h) {
            case 0:
                r = (int32_t) (brightness * 255.0f + 0.5f);
                g = (int32_t) (t * 255.0f + 0.5f);
                b = (int32_t) (p * 255.0f + 0.5f);
                break;
            case 1:
                r = (int32_t) (q * 255.0f + 0.5f);
                g = (int32_t) (brightness * 255.0f + 0.5f);
                b = (int32_t) (p * 255.0f + 0.5f);
                break;
            case 2:
                r = (int32_t) (p * 255.0f + 0.5f);
                g = (int32_t) (brightness * 255.0f + 0.5f);
                b = (int32_t) (t * 255.0f + 0.5f);
                break;
            case 3:
                r = (int32_t) (p * 255.0f + 0.5f);
                g = (int32_t) (q * 255.0f + 0.5f);
                b = (int32_t) (brightness * 255.0f + 0.5f);
                break;
            case 4:
                r = (int32_t) (t * 255.0f + 0.5f);
                g = (int32_t) (p * 255.0f + 0.5f);
                b = (int32_t) (brightness * 255.0f + 0.5f);
                break;
            case 5:
                r = (int32_t) (brightness * 255.0f + 0.5f);
                g = (int32_t) (p * 255.0f + 0.5f);
                b = (int32_t) (q * 255.0f + 0.5f);
                break;
        }
    }
    return 0xff000000 | (r << 16) | (g << 8) | (b << 0);
}

int32_t _mbrotCalc0PixelColour(int32_t iterCount, double z) {
    return _HSVToRGB((iterCount + 1 - log2(log(z) / 2)) / mbrotParams->maxIterations, 0.999, 0.999);
}

int32_t _mbrotCalc1PixelColour(int32_t iterCount, double z) {
    return _HSVToRGB((iterCount + 1 - log2(log(z) / 2)) / (mbrotParams->maxIterations * 3) - 1 / 6.0f, 1, 1);
}

int32_t _mbrotCalc2PixelColour(int32_t iterCount, double z) {
    return _HSVToRGB(1 / 6.0f + (iterCount + 1 - log2(log(z) / 2)) / (mbrotParams->maxIterations * 3), 1, 1);
}

int32_t _mbrotCalc3PixelColour(int32_t iterCount, double z) {
    return _HSVToRGB(0.5 + (iterCount + 1 - log2(log(z) / 2)) / (mbrotParams->maxIterations * 3), 1, 1);
}

int32_t _mbrotCalc4PixelColour(int32_t iterCount, double z) {
    return iterCount;
}

void _mbrotRenderArbPrec() {

}

void *_mbrotRenderSubregPri(void *data) {
    int32_t i, j, k, *stats; // stats[] holds: min iterations, max iterations, total iterations, convergent points.
    double cR, cI, zR, zI, temp;
    mbrot_subreg_pri subreg;

    subreg = *((mbrot_subreg_pri *) data);
    subreg.minX += 0.5 * mbrotPlaneUnitXPri;
    subreg.minY += 0.5 * mbrotPlaneUnitYPri;

    stats = malloc(4 * sizeof (int32_t));
    stats[0] = mbrotParams->maxIterations;
    stats[1] = 0;
    stats[2] = 0;
    stats[3] = 0;

    for (i = 0; i < subreg.sizeX; i++) {
        for (j = 0; j < subreg.sizeY; j++) {
            zR = cR = subreg.minX + mbrotPlaneUnitXPri * i;
            zI = cI = subreg.minY + mbrotPlaneUnitYPri * j;
            k = 0;

            while (k < mbrotParams->maxIterations) {
                if (zR * zR + zI * zI > (double) 25) {
                    break;
                }

                temp = zR;
                zR = zR * zR - zI * zI + cR;
                zI = 2 * temp * zI + cI;
                k++;
            }

            stats[2] += k;
            if (k < mbrotParams->maxIterations && k > stats[1]) {
                stats[1] = k;
            }
            if (k > 0 && k < stats[0]) {
                stats[0] = k;
            }

            if (k == mbrotParams->maxIterations) {
                stats[3]++;
                *(mbrotParams->raster + ((mbrotParams->rasterSizeX * (subreg.minPixelY + j)) + subreg.minPixelX
                        + i)) = 0xff000000;
                continue;
            }
            *(mbrotParams->raster + ((mbrotParams->rasterSizeX * (subreg.minPixelY + j)) + subreg.minPixelX
                    + i)) = (*mbrotColFunc)(k, zR * zR + zI * zI);
        }
    }
    free(data);

    return (void *) stats;
}

void _mbrotRenderPri() {
    int32_t i, j, *stats;
    mbrot_subreg_pri *subreg;
    double planeMinX, planeMinY;
    pthread_t threads[16]; // Fix: modify to make compatible with numProcCores != 4

    planeMinX = mpfr_get_d(mbrotPlaneMinX, MPFR_RNDN);
    planeMinY = mpfr_get_d(mbrotPlaneMinY, MPFR_RNDN);
    mbrotPlaneUnitXPri = mpfr_get_d(mbrotPlaneUnitX, MPFR_RNDN);
    mbrotPlaneUnitYPri = mpfr_get_d(mbrotPlaneUnitY, MPFR_RNDN);

    (*(mbrotParams->eventHandler))(MBROT_EVT_RENDERING_BEGUN, NULL);
    _toggleTimer();

    for (i = 0; i < 3; i++) {
        for (j = 0; j < 3; j++) {
            subreg = malloc(sizeof (mbrot_subreg_pri));
            subreg->sizeX = mbrotParams->rasterSizeX / 4;
            subreg->minPixelX = i * subreg->sizeX;
            subreg->sizeY = mbrotParams->rasterSizeY / 4;
            subreg->minPixelY = j * subreg->sizeY;
            subreg->minX = planeMinX + mbrotPlaneUnitXPri * subreg->minPixelX;
            subreg->minY = planeMinY + mbrotPlaneUnitYPri * subreg->minPixelY;

            pthread_create(&threads[4 * j + i], NULL, _mbrotRenderSubregPri, (void *) subreg);
            usleep(10000);
            
        }
    }

    for (i = 3, j = 0; j < 3; j++) {
        subreg = malloc(sizeof (mbrot_subreg_pri));
        subreg->sizeX = mbrotParams->rasterSizeX / 4;
        subreg->minPixelX = i * subreg->sizeX;
        subreg->sizeX = mbrotParams->rasterSizeX - subreg->minPixelX;
        subreg->sizeY = mbrotParams->rasterSizeY / 4;
        subreg->minPixelY = j * subreg->sizeY;
        subreg->minX = planeMinX + mbrotPlaneUnitXPri * subreg->minPixelX;
        subreg->minY = planeMinY + mbrotPlaneUnitYPri * subreg->minPixelY;

        pthread_create(&threads[4 * j + i], NULL, _mbrotRenderSubregPri, (void *) subreg);
        usleep(10000);
    }

    for (i = 0, j = 3; i < 3; i++) {
        subreg = malloc(sizeof (mbrot_subreg_pri));
        subreg->sizeX = mbrotParams->rasterSizeX / 4;
        subreg->minPixelX = i * subreg->sizeX;
        subreg->sizeY = mbrotParams->rasterSizeY / 4;
        subreg->minPixelY = j * subreg->sizeY;
        subreg->sizeY = mbrotParams->rasterSizeY - subreg->minPixelY;
        subreg->minX = planeMinX + mbrotPlaneUnitXPri * subreg->minPixelX;
        subreg->minY = planeMinY + mbrotPlaneUnitYPri * subreg->minPixelY;

        pthread_create(&threads[4 * j + i], NULL, _mbrotRenderSubregPri, (void *) subreg);
        usleep(10000);
    }

    subreg = malloc(sizeof (mbrot_subreg_pri));
    subreg->sizeX = mbrotParams->rasterSizeX / 4;
    subreg->minPixelX = 3 * subreg->sizeX;
    subreg->sizeX = mbrotParams->rasterSizeX - subreg->minPixelX;
    subreg->sizeY = mbrotParams->rasterSizeY / 4;
    subreg->minPixelY = 3 * subreg->sizeY;
    subreg->sizeY = mbrotParams->rasterSizeY - subreg->minPixelY;
    subreg->minX = planeMinX + mbrotPlaneUnitXPri * subreg->minPixelX;
    subreg->minY = planeMinY + mbrotPlaneUnitYPri * subreg->minPixelY;

    pthread_create(&threads[15], NULL, _mbrotRenderSubregPri, (void *) subreg);

    mbrotStats->minIterations = mbrotParams->maxIterations;
    mbrotStats->maxIterations = 0;

    for (i = 0; i < 16; i++) {
        pthread_join(threads[i], (void **) &stats);

        if (mbrotStats->minIterations > stats[0]) {
            mbrotStats->minIterations = stats[0];
        }
        if (mbrotStats->maxIterations < stats[1]) {
            mbrotStats->maxIterations = stats[1];
        }
        mbrotStats->meanIterations += stats[2];
        mbrotStats->convergentPoints += stats[3];

        free(stats);
    }
    mbrotStats->meanIterations /= (mbrotParams->rasterSizeX * mbrotParams->rasterSizeY);
    mbrotStats->renderingTime = _toggleTimer();
    (*(mbrotParams->eventHandler))(MBROT_EVT_RENDERING_ENDED, NULL);
}

void *_mbrotBuddhaProcessSample(void *data) {
    int i, j, x, y;
    double cR, cI, zR, zI, calcMinX, calcMinY, maxX, maxY, temp;
    mbrot_buddha_sample *sample;

    sample = (mbrot_buddha_sample *) data;

    maxX = sample->minX + mbrotParams->rasterSizeX * mbrotPlaneUnitXPri;
    maxY = sample->minY + mbrotParams->rasterSizeY * mbrotPlaneUnitYPri;
    calcMinX = sample->minX + mbrotPlaneUnitXPri * 0.5;
    calcMinY = sample->minY + mbrotPlaneUnitYPri * 0.5;

    for (i = 0; i < sample->bufferSize; i++) {
        sample->buffer[i] = 0;
    }

    for (i = 0; i < sample->sampleSize; i++) {
        zR = cR = sample->sample[2 * i];
        zI = cI = sample->sample[2 * i + 1];

        j = 0;
        while (j < mbrotParams->maxIterations) {
            if (zR * zR + zI * zI > (double) 4) {
                break;
            }

            temp = zR;
            zR = zR * zR - zI * zI + cR;
            zI = 2 * temp * zI + cI;
            j++;
        }

        if (j < mbrotParams->maxIterations && j > mbrotParams->maxIterations / 10) {
            zR = cR;
            zI = cI;
            j = 0;

            while (j++ < 5) {
                temp = zR;
                zR = zR * zR - zI * zI + cR;
                zI = 2 * temp * zI + cI;
            }

            while (j < mbrotParams->maxIterations) {
                temp = zR;
                zR = zR * zR - zI * zI + cR;
                zI = 2 * temp * zI + cI;

                if (zR >= sample->minX && zI >= sample->minY && zR <= maxX && zI <= maxY) {
                    x = (int32_t) ((zR - calcMinX) / mbrotPlaneUnitXPri + 0.5);
                    y = (int32_t) ((zI - calcMinY) / mbrotPlaneUnitYPri + 0.5);
                    sample->buffer[mbrotParams->rasterSizeX * y + x]++;
                }

                j++;
            }
        }
    }
    return NULL;
}

void _mbrotBuddhaRender() {
    int32_t i, j, k, maxPixelValue, rasterSize, threadSampleSize, numThreads, *stats;
    mbrot_buddha_sample ** samples;
    double planeMinX, planeMinY, planeRangeX, planeRangeY, temp;
    pthread_t *threads;
    gsl_rng *g;

    numThreads = mbrotParams->numProcCores * 4;
    samples = malloc(sizeof (mbrot_buddha_sample *) * numThreads);
    threads = malloc(sizeof (pthread_t) * numThreads);
    planeMinX = mpfr_get_d(mbrotPlaneMinX, MPFR_RNDN);
    planeMinY = mpfr_get_d(mbrotPlaneMinY, MPFR_RNDN);
    mbrotPlaneUnitXPri = mpfr_get_d(mbrotPlaneUnitX, MPFR_RNDN);
    mbrotPlaneUnitYPri = mpfr_get_d(mbrotPlaneUnitY, MPFR_RNDN);
    planeRangeX = mbrotParams->rasterSizeX * mbrotPlaneUnitXPri;
    planeRangeY = mbrotParams->rasterSizeY * mbrotPlaneUnitYPri;

    rasterSize = mbrotParams->rasterSizeX * mbrotParams->rasterSizeY;
    threadSampleSize = (int32_t) (mbrotParams->sampleSize / numThreads);
    g = gsl_rng_alloc(gsl_rng_mt19937_1999);

    (*(mbrotParams->eventHandler))(MBROT_EVT_RENDERING_BEGUN, NULL);
    _toggleTimer();

    for (i = 0; i < numThreads; i++) {
        samples[i] = malloc(sizeof (mbrot_buddha_sample));
        samples[i]->buffer = (uint16_t *) malloc(rasterSize * sizeof (uint16_t));
        samples[i]->bufferSize = rasterSize;
        samples[i]->minX = planeMinX;
        samples[i]->minY = planeMinY;
        if (i < numThreads - 1) {
            samples[i]->sample = malloc(sizeof (double) * 2 * threadSampleSize);
            samples[i]->sampleSize = threadSampleSize;
            for (j = 0; j < threadSampleSize; j++) {
                samples[i]->sample[2 * j] = planeMinX + gsl_rng_uniform(g) * planeRangeX;
                samples[i]->sample[2 * j + 1] = planeMinY + gsl_rng_uniform(g) * planeRangeY;
            }
        } else {
            samples[i]->sample = malloc(sizeof (double) * 2 * (mbrotParams->sampleSize - threadSampleSize
                    * (numThreads - 1)));
            samples[i]->sampleSize = mbrotParams->sampleSize - threadSampleSize * (numThreads - 1);
            j = samples[i]->sampleSize;
            while (j-- > 0) {
                samples[i]->sample[2 * j] = planeMinX + gsl_rng_uniform(g) * planeRangeX;
                samples[i]->sample[2 * j + 1] = planeMinY + gsl_rng_uniform(g) * planeRangeY;
            }
        }

        pthread_create(&threads[i], NULL, _mbrotBuddhaProcessSample, (void *) samples[i]);
        usleep(10000);
    }
    for (i = 0; i
            < numThreads; i++) {
        pthread_join(threads[i], (void **) &stats);
    }


    maxPixelValue = 0;
    for (i = 0; i < rasterSize; i++) {
        for (j = 0; j < numThreads; j++) {
            mbrotParams->raster[i] += samples[j]->buffer[i];
        }
        if (maxPixelValue < mbrotParams->raster[i]) {
            maxPixelValue = mbrotParams->raster[i];
        }
    }

    for (i = 0; i < rasterSize; i++) {
        temp = tanh(mbrotParams->raster[i] / (double) maxPixelValue * M_PI);
        temp = tanh(temp * M_PI);
        mbrotParams->raster[i] = _HSVToRGB(0.69 - temp / 4, 0.6 + temp * 0.3, temp);
    }
    mbrotStats->renderingTime = _toggleTimer();
    (*(mbrotParams->eventHandler))(MBROT_EVT_RENDERING_ENDED, NULL);

    for (i = 0; i < numThreads; i++) {

        free(samples[i]->buffer);
        free(samples[i]->sample);
    }
    free(threads);
    free(samples);
}

void mbrotInitialize(mbrot_params * p) {

    mpfr_prec_t prec;

    mbrotParams = p;
    mbrotStats = (mbrot_stats *) malloc(sizeof (mbrot_stats));
    prec = mbrotParams->useArbitraryPrecision ? (mpfr_prec_t) mbrotParams->precision : DEFAULT_PRECISION;
    mbrotStats->convergentPoints = mbrotStats->maxIterations = mbrotStats->minIterations = 0;
    mbrotStats->renderingTime = mbrotStats->meanIterations = 0;

    mpfr_inits2(prec, mbrotPlaneMinX, mbrotPlaneMaxX, mbrotPlaneMinY, mbrotPlaneMaxY,
            mbrotPlaneUnitX, mbrotPlaneUnitY, NULL);
    mpfr_set_str(mbrotPlaneMinX, mbrotParams->planeMinX, 10, MPFR_RNDN);
    mpfr_set_str(mbrotPlaneMaxX, mbrotParams->planeMaxX, 10, MPFR_RNDN);
    mpfr_set_str(mbrotPlaneMinY, mbrotParams->planeMinY, 10, MPFR_RNDN);
    mpfr_set_str(mbrotPlaneMaxY, mbrotParams->planeMaxY, 10, MPFR_RNDN);
    mpfr_sub(mbrotPlaneUnitX, mbrotPlaneMaxX, mbrotPlaneMinX, MPFR_RNDN);
    mpfr_div_si(mbrotPlaneUnitX, mbrotPlaneUnitX, mbrotParams->rasterSizeX, MPFR_RNDN);
    mpfr_sub(mbrotPlaneUnitY, mbrotPlaneMaxY, mbrotPlaneMinY, MPFR_RNDN);
    mpfr_div_si(mbrotPlaneUnitY, mbrotPlaneUnitY, mbrotParams->rasterSizeY, MPFR_RNDN);
}

mbrot_stats * mbrotRender() {
    switch (mbrotParams->mbrotVariant) {
        case MBROT_VAR_REGULAR:
            switch (mbrotParams->colouringMethod) {
                case MBROT_COL_METH_REGULAR:
                    mbrotColFunc = &_mbrotCalc0PixelColour;
                    break;
                case MBROT_COL_METH_RED:
                    mbrotColFunc = &_mbrotCalc1PixelColour;
                    break;
                case MBROT_COL_METH_GREEN:
                    mbrotColFunc = &_mbrotCalc2PixelColour;
                    break;
                case MBROT_COL_METH_BLUE:
                    mbrotColFunc = &_mbrotCalc3PixelColour;
                    break;
                default:
                    mbrotColFunc = &_mbrotCalc0PixelColour;
                    break;
            }
            if (mbrotParams->useArbitraryPrecision) {
                _mbrotRenderArbPrec();
            } else {
                _mbrotRenderPri();
            }
            break;
        case MBROT_VAR_BUDDHABROT:
            _mbrotBuddhaRender();

            break;
    }

    return mbrotStats;
}

void mbrotFinalize() {
    mpfr_clears(mbrotPlaneMinX, mbrotPlaneMaxX, mbrotPlaneMinY, mbrotPlaneMaxY, mbrotPlaneUnitX,
            mbrotPlaneUnitY, NULL);
    free(mbrotStats);
}
