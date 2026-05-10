#!/bin/bash
echo "Creating S3 bucket..."
awslocal s3 mb s3://docmanager-bucket
echo "Bucket created."
