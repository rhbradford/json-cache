// Author: Richard Bradford
const webpack = require('webpack')

const path = require('path');

module.exports = {
    plugins: [
        // Necessary b/c golden-layout depends on all 3 of these libs via UMD globals
        new webpack.ProvidePlugin({
            React: 'react',
            ReactDOM: 'react-dom',
            $: 'jquery',
            jQuery: 'jquery'
        })
    ],
    resolve: {
        extensions: ['.js', '.ts', '.tsx']
    },
    module: {
        rules:   [
            {
                test:    /\.ts$|\.tsx$/,
                include: path.resolve(__dirname, '../'),
                use:     [
                    {
                        loader: 'ts-loader'
                    }
                ]
            }
            , {
                test:    /\.woff2?$|\.ttf$|\.eot$|\.svg$|\.png$/,
                include: path.resolve(__dirname, '../'),
                use:     [
                    {
                        loader: 'url-loader'
                    }
                ]
            }
            , {
                test:    /\.css$/,
                include: path.resolve(__dirname, '../'),
                use:     [
                    {
                        loader: 'style-loader'
                    },
                    {
                        loader:  'css-loader',
                        options: {
                            // Re-direct Semantic UI CSS theme to find assets from installed semantic-ui-css package
                            alias: {
                                './themes/default': 'semantic-ui-css/themes/default'
                            }
                        }
                    }
                ]
            }
        ]
    }
}

